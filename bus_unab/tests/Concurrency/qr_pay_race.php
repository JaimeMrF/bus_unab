<?php

/**
 * Prueba de concurrencia REAL del cobro QR (requiere MySQL/InnoDB; SQLite no tiene FOR UPDATE).
 * NO es parte de `php artisan test`. Uso (base de pruebas dedicada, ya migrada):
 *   DB_CONNECTION=mysql DB_DATABASE=bus_qa_concurrency DB_USERNAME=root APP_ENV=testing \
 *     php tests/Concurrency/qr_pay_race.php [procesos=8] [rondas=5]
 * Crea datos propios (prefijo qa_race_) y verifica: 1 solo cobro por QR, saldo y ledger coherentes.
 */
require __DIR__.'/../../vendor/autoload.php';

$app = require __DIR__.'/../../bootstrap/app.php';
$app->make(Kernel::class)->bootstrap();

use App\Models\User;
use App\Models\Wallet;
use App\Models\WalletTransaction;
use App\Services\QrPaymentService;
use App\Services\WalletService;
use Illuminate\Contracts\Console\Kernel;
use Illuminate\Support\Facades\DB;

if (DB::getDriverName() !== 'mysql' || ! str_contains((string) DB::getDatabaseName(), 'qa')) {
    fwrite(STDERR, "Rechazado: solo MySQL y bases con 'qa' en el nombre.\n");
    exit(2);
}

if (($argv[1] ?? '') === 'debit-worker') {
    [, , $walletId, $ref, $startAt] = $argv;
    while (microtime(true) < (float) $startAt) {
        usleep(200);
    }
    try {
        app(WalletService::class)->debit(Wallet::findOrFail((int) $walletId), QrPaymentService::FALLBACK_FARE_CENTAVOS, $ref, 'qa');
        echo 'OK
';
    } catch (Throwable $e) {
        echo 'ERR '.class_basename($e).'
';
    }
    exit(0);
}

if (($argv[1] ?? '') === 'worker') {
    [, , $qr, $driverId, $startAt] = $argv;
    while (microtime(true) < (float) $startAt) {
        usleep(200);
    }
    try {
        $driver = User::findOrFail((int) $driverId);
        app(QrPaymentService::class)->pay($qr, $driver);
        echo "OK\n";
    } catch (Throwable $e) {
        echo 'ERR '.class_basename($e).': '.$e->getMessage()."\n";
    }
    exit(0);
}

$procs = (int) ($argv[1] ?? 8);
$rounds = (int) ($argv[2] ?? 5);
$fare = QrPaymentService::FALLBACK_FARE_CENTAVOS;
$qrSvc = app(QrPaymentService::class);
$walletSvc = app(WalletService::class);
$failures = 0;

for ($r = 1; $r <= $rounds; $r++) {
    $tag = 'qa_race_'.uniqid();
    $pax = User::factory()->create(['role' => 'pasajero', 'email' => "$tag@t.co"]);
    $drivers = [];
    for ($i = 0; $i < $procs; $i++) {
        $drivers[] = User::factory()->create(['role' => 'driver', 'email' => "{$tag}_d$i@t.co"]);
    }
    // Saldo para UN solo viaje: si se cobra dos veces, el saldo no alcanza o queda negativo.
    $walletSvc->credit(Wallet::para($pax), $fare, "seed_$tag", 'qa');
    [$token, $qr] = $qrSvc->issueToken($pax);

    $startAt = microtime(true) + 1.5;
    $handles = [];
    foreach ($drivers as $d) {
        $cmd = [PHP_BINARY, __FILE__, 'worker', $qr, (string) $d->id, (string) $startAt];
        $p = proc_open($cmd, [1 => ['pipe', 'w'], 2 => ['pipe', 'w']], $pipes, null, null);
        $handles[] = [$p, $pipes];
    }
    $out = [];
    foreach ($handles as [$p, $pipes]) {
        $out[] = trim(stream_get_contents($pipes[1]).stream_get_contents($pipes[2]));
        proc_close($p);
    }

    $ok = count(array_filter($out, fn ($o) => str_starts_with($o, 'OK')));
    $debits = WalletTransaction::where('reference', "pay_{$token->id}")->count();
    $w = Wallet::para($pax)->fresh();
    $ledger = $walletSvc->ledgerSum($w);
    $unexpected = array_filter($out, fn ($o) => ! str_starts_with($o, 'OK') && ! str_contains($o, 'ya fue utilizado') && ! str_contains($o, 'DomainException'));

    $good = $ok === 1 && $debits === 1 && (int) $w->balance_centavos === 0 && $ledger === 0 && ! $unexpected;
    printf("ronda %d: ok=%d debitos=%d saldo=%d ledger=%d inesperados=%d %s\n", $r, $ok, $debits, $w->balance_centavos, $ledger, count($unexpected), $good ? 'PASS' : 'FAIL');
    if (! $good) {
        $failures++;
        print_r(array_count_values($out));
    }
}

// Escenario 2: N débitos concurrentes (referencias distintas) sobre una wallet con saldo para UNO.
for ($r = 1; $r <= $rounds; $r++) {
    $tag = 'qa_race_dbt_'.uniqid();
    $pax = User::factory()->create(['role' => 'pasajero', 'email' => "$tag@t.co"]);
    $wallet = Wallet::para($pax);
    $walletSvc->credit($wallet, $fare, "seed_$tag", 'qa');
    $startAt = microtime(true) + 1.5;
    $handles = [];
    for ($i = 0; $i < $procs; $i++) {
        $cmd = [PHP_BINARY, __FILE__, 'debit-worker', (string) $wallet->id, "{$tag}_$i", (string) $startAt];
        $handles[] = [proc_open($cmd, [1 => ['pipe', 'w'], 2 => ['pipe', 'w']], $pipes), $pipes];
    }
    $out = [];
    foreach ($handles as [$p, $pipes]) {
        $out[] = trim(stream_get_contents($pipes[1]).stream_get_contents($pipes[2]));
        proc_close($p);
    }
    $ok = count(array_filter($out, fn ($o) => $o === 'OK'));
    $insufficient = count(array_filter($out, fn ($o) => $o === 'ERR InsufficientFundsException'));
    $w = $wallet->fresh();
    $good = $ok === 1 && $insufficient === $procs - 1 && (int) $w->balance_centavos === 0 && $walletSvc->ledgerSum($w) === 0;
    printf('debito-concurrente %d: ok=%d saldo_insuficiente=%d saldo=%d %s
', $r, $ok, $insufficient, $w->balance_centavos, $good ? 'PASS' : 'FAIL');
    if (! $good) {
        $failures++;
        print_r(array_count_values($out));
    }
}

echo $failures === 0 ? "RESULTADO: PASS ($rounds rondas x $procs procesos)\n" : "RESULTADO: FAIL ($failures rondas)\n";
exit($failures === 0 ? 0 : 1);

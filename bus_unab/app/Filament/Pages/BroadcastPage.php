<?php

namespace App\Filament\Pages;

use App\Services\NotificationService;
use Filament\Actions\Action;
use Filament\Forms\Components\Select;
use Filament\Forms\Components\Textarea;
use Filament\Forms\Components\TextInput;
use Filament\Forms\Concerns\InteractsWithForms;
use Filament\Forms\Contracts\HasForms;
use Filament\Forms\Form;
use Filament\Notifications\Notification;
use Filament\Pages\Page;
use Illuminate\Support\Facades\Log;

class BroadcastPage extends Page implements HasForms
{
    use InteractsWithForms;

    protected static ?string $navigationIcon  = 'heroicon-o-megaphone';
    protected static ?string $navigationLabel = 'Broadcast';
    protected static ?string $title           = 'Enviar Notificación Global';
    protected static ?int    $navigationSort  = 10;
    protected static string  $view            = 'filament.pages.broadcast-page';

    public ?array $data = [];

    public function mount(): void
    {
        $this->form->fill([
            'role' => 'all',
        ]);
    }

    public function form(Form $form): Form
    {
        return $form
            ->schema([
                TextInput::make('title')
                    ->label('Título')
                    ->required()
                    ->maxLength(255)
                    ->placeholder('Ej: Aviso importante'),

                Textarea::make('body')
                    ->label('Mensaje')
                    ->required()
                    ->maxLength(1000)
                    ->rows(4)
                    ->placeholder('Escribe el mensaje que recibirán los usuarios...'),

                Select::make('role')
                    ->label('Destinatarios')
                    ->options([
                        'all'     => 'Todos los usuarios',
                        'student' => 'Solo estudiantes',
                        'driver'  => 'Solo conductores',
                    ])
                    ->default('all')
                    ->required(),
            ])
            ->statePath('data');
    }

    protected function getFormActions(): array
    {
        return [
            Action::make('send')
                ->label('Enviar notificación')
                ->icon('heroicon-o-paper-airplane')
                ->color('primary')
                ->action('send'),
        ];
    }

    public function send(): void
    {
        $data = $this->form->getState();

        try {
            app(NotificationService::class)->broadcast(
                title: $data['title'],
                body:  $data['body'],
                data:  ['type' => 'general'],
                role:  $data['role'],
            );

            Notification::make()
                ->title('Notificación enviada')
                ->body("Broadcast enviado a: {$data['role']}")
                ->success()
                ->send();

            $this->form->fill(['role' => 'all']);

        } catch (\Exception $e) {
            Log::error('BroadcastPage error', ['error' => $e->getMessage()]);

            Notification::make()
                ->title('Error al enviar')
                ->body($e->getMessage())
                ->danger()
                ->send();
        }
    }
}

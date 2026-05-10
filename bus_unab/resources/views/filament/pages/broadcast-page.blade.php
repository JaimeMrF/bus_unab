<x-filament-panels::page>
    <x-filament::section>
        <x-slot name="heading">Enviar notificación push</x-slot>
        <x-slot name="description">
            El mensaje llegará como notificación push a los dispositivos registrados.
        </x-slot>

        <form wire:submit="send">
            {{ $this->form }}

            <div class="mt-6 flex gap-3">
                <x-filament::button type="submit" icon="heroicon-o-paper-airplane">
                    Enviar notificación
                </x-filament::button>
            </div>
        </form>

        <x-filament-actions::modals />
    </x-filament::section>
</x-filament-panels::page>

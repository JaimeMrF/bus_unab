<x-filament-widgets::widget>
    <x-filament::section>
        <x-slot name="heading">
            <div class="flex items-center gap-2">
                <div class="p-2 bg-blue-100 dark:bg-blue-900 rounded-lg">
                    <svg class="w-5 h-5 text-blue-600 dark:text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                </div>
                <div>
                    <h2 class="text-lg font-bold tracking-tight">Calendario de Rutas</h2>
                    <p class="text-xs text-gray-500 dark:text-gray-400">Sincronización con Google Calendar</p>
                </div>
            </div>
        </x-slot>

        <div class="relative w-full overflow-hidden rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-inner" style="height: 600px;">
            <iframe 
                src="{{ $this->getCalendarUrl() }}" 
                style="border: 0" 
                width="100%" 
                height="100%" 
                frameborder="0" 
                scrolling="no"
                class="dark:invert dark:hue-rotate-180 opacity-90 hover:opacity-100 transition-opacity duration-300"
            ></iframe>
            
            {{-- Overlay para cuando no hay ID configurado o es el default --}}
            @if(env('GOOGLE_CALENDAR_ID') === null)
                <div class="absolute inset-0 flex flex-col items-center justify-center bg-gray-50/80 dark:bg-gray-800/80 backdrop-blur-sm pointer-events-none">
                    <div class="p-4 text-center">
                        <p class="text-sm font-medium text-gray-600 dark:text-gray-300 mb-2">Para ver tu calendario real:</p>
                        <code class="px-2 py-1 bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded text-xs text-blue-600">GOOGLE_CALENDAR_ID=tu_email@gmail.com</code>
                        <p class="text-[10px] text-gray-400 mt-2">Configura esto en tu archivo .env</p>
                    </div>
                </div>
            @endif
        </div>
    </x-filament::section>
</x-filament-widgets::widget>

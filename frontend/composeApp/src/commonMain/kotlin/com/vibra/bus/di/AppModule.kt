package com.vibra.bus.di

import com.russhwolf.settings.Settings
import com.vibra.bus.data.api.AuthApi
import com.vibra.bus.data.api.BusApi
import com.vibra.bus.data.api.PoiApi
import com.vibra.bus.data.api.RequestApi
import com.vibra.bus.data.api.StopApi
import com.vibra.bus.data.api.WalletApi
import com.vibra.bus.data.api.createKtorClient
import com.vibra.bus.data.repository.AuthRepository
import com.vibra.bus.data.repository.BusRepository
import com.vibra.bus.data.repository.PoiRepository
import com.vibra.bus.data.repository.RequestRepository
import com.vibra.bus.data.repository.StopRepository
import com.vibra.bus.data.repository.WalletRepository
import com.vibra.bus.presentation.viewmodel.AuthViewModel
import com.vibra.bus.presentation.viewmodel.DriverModeViewModel
import com.vibra.bus.presentation.viewmodel.HomeViewModel
import com.vibra.bus.presentation.viewmodel.MyQRViewModel
import com.vibra.bus.presentation.viewmodel.MyTripsViewModel
import com.vibra.bus.presentation.viewmodel.NotificationsViewModel
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import com.vibra.bus.presentation.viewmodel.StopSelectionViewModel
import com.vibra.bus.presentation.viewmodel.WalletViewModel
import com.vibra.bus.util.AppSettings
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformModule(): Module

val commonModule = module {
    // Settings
    single { Settings() }
    single { AppSettings(get()) }

    // Ktor
    single { createKtorClient(get()) }

    // APIs
    single { AuthApi(get()) }
    single { BusApi(get()) }
    single { StopApi(get()) }
    single { RequestApi(get()) }
    single { PoiApi(get()) }
    single { com.vibra.bus.data.api.GoogleMapsApi(get()) }
    single { WalletApi(get()) }

    // Repositories
    single { AuthRepository(get(), get()) }
    single { BusRepository(get()) }
    single { StopRepository(get()) }
    single { RequestRepository(get(), get()) }
    single { PoiRepository(get()) }
    single { WalletRepository(get()) }

    // ViewModels
    viewModel { AuthViewModel(get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { StopSelectionViewModel(get(), get()) }
    viewModel { MyQRViewModel(get()) }
    viewModel { MyTripsViewModel(get(), get()) }
    viewModel { NotificationsViewModel(get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { DriverModeViewModel(get(), get(), get()) }
    viewModel { com.vibra.bus.presentation.viewmodel.WaitingBusViewModel(get()) }
    viewModel { com.vibra.bus.presentation.viewmodel.QRScannerViewModel(get(), get()) }
    viewModel { WalletViewModel(get()) }
}

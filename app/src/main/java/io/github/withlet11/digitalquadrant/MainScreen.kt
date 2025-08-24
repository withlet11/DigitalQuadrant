package io.github.withlet11.digitalquadrant

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

sealed interface MainNavigation {
    @Serializable
    data class QuadrantScreen(val index: Int)

    @Serializable
    object License

    @Serializable
    object OssLicense

    @Serializable
    data class OssLicenseDetails(val name: String, val terms: String)
}

class MyViewModel : ViewModel() {
    private val _isAutoHoldEnabled = MutableStateFlow(false)
    val isAutoHoldEnabled: StateFlow<Boolean> = _isAutoHoldEnabled.asStateFlow()

    fun toggleAutoHold() {
        _isAutoHoldEnabled.update { !it }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenAppBar(
    currentScreen: NavDestination?,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    navController: NavHostController,
    isAutoHoldEnabled: Boolean,
    onToggleAutoHold: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Text(
                text = stringResource(
                    when {
                        currentScreen == null -> R.string.app_name
                        currentScreen.hasRoute(MainNavigation.License::class) -> R.string.license
                        currentScreen.hasRoute(MainNavigation.OssLicense::class) -> R.string.opensource_licenses
                        currentScreen.hasRoute(MainNavigation.OssLicenseDetails::class) -> R.string.opensource_licenses
                        else -> R.string.app_name
                    }
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            if (!canNavigateBack) {
                AutoHoldSwitch(
                    isChecked = isAutoHoldEnabled,
                    onCheckedChange = { onToggleAutoHold() })
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Localized description"
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.license)) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            navController.navigate(MainNavigation.License)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.opensource_licenses)) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            navController.navigate(MainNavigation.OssLicense)
                            expanded = false
                        }
                    )
                }
            }
        },
    )
}

@Composable
fun MainScreen(
    context: Context,
    licensesStateFlow: StateFlow<OssLicenseList>,
    navController: NavHostController = rememberNavController(),
    myViewModel: MyViewModel = viewModel()
) {
    val isAutoHoldEnabled by myViewModel.isAutoHoldEnabled.collectAsState()
    val vibrator: Vibrator
    val vibratorManager: VibratorManager
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = backStackEntry?.destination


    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibrator = vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    Scaffold(
        topBar = {
            MainScreenAppBar(
                currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                navController = navController,
                isAutoHoldEnabled,
                onToggleAutoHold = { myViewModel.toggleAutoHold() }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = MainNavigation.QuadrantScreen(0),
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<MainNavigation.QuadrantScreen> { backStackEntry ->
                val index = backStackEntry.toRoute<MainNavigation.QuadrantScreen>().index
                QuadrantScreen(index, isAutoHoldEnabled, vibrator)
            }

            composable<MainNavigation.License> {
                LicenceScreen()
            }
            composable<MainNavigation.OssLicense> {
                OSSLicenseListScreen(
                    navController = navController,
                    licensesStateFlow = licensesStateFlow
                )
            }
            composable<MainNavigation.OssLicenseDetails> { backStackEntry ->
                val name = backStackEntry.toRoute<MainNavigation.OssLicenseDetails>().name
                val terms = backStackEntry.toRoute<MainNavigation.OssLicenseDetails>().terms

                OssLicenseDetailScreen(name = name, terms = terms)
            }
        }
    }
}
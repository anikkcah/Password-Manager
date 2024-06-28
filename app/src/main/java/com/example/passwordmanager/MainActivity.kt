package com.example.passwordmanager

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.passwordmanager.db.UserCredentials
import com.example.passwordmanager.db.repository.UserCredentialsRepository
import com.example.passwordmanager.security.BiometricPromptManager
import com.example.passwordmanager.ui.theme.PasswordManagerTheme
import com.example.passwordmanager.viewmodel.CredentialsViewModel
import com.example.passwordmanager.viewmodel.CredentialsViewModelFactory
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory


class MainActivity : AppCompatActivity() {

    private val promptManager by lazy {
        BiometricPromptManager(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {

            PasswordManagerTheme {

                BiometricUnlock(appContext = applicationContext, promptManager = promptManager)


            }

        }
    }


    @Composable
    fun BiometricUnlock(appContext: Context, promptManager: BiometricPromptManager) {
        val lifecycleOwner = LocalLifecycleOwner.current

        val biometricResult by promptManager.promptResults.collectAsState(initial = null)

        val enrollLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                println("Activity result: $it")
            }
        )

        LaunchedEffect(biometricResult) {
            if (biometricResult is BiometricPromptManager.BiometricResult.AuthenticationNotSet) {
                if (Build.VERSION.SDK_INT >= 30) {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                        )
                    }
                    enrollLauncher.launch(enrollIntent)
                }
            }
        }

        biometricResult?.let { result ->

            when (result) {

                is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                    result.error
                    ErrorDisplay()
                }

                BiometricPromptManager.BiometricResult.AuthenticationFailed -> {

                }

                BiometricPromptManager.BiometricResult.AuthenticationNotSet -> {
                    "Authentication not set"
                }

                BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                    "Authentication success"
                    MyApp(appContext = appContext, promptManager = promptManager)
                }

                BiometricPromptManager.BiometricResult.FeatureUnavailable -> {
                    "Feature unavailable"
                }

                BiometricPromptManager.BiometricResult.HardwareUnavailable -> {
                    "Hardware unavailable"
                }


            }


        }


        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    promptManager.showBiometricPrompt(
                        title = "Unlock Password Manager",
                        description = "Unlock your screen with PIN or fingerprint"
                    )
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }


    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopAppBar() {
        androidx.compose.material3.TopAppBar(title = { Text(text = "Password Manager") })
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MyApp(appContext: Context, promptManager: BiometricPromptManager) {


        var showBottomSheet by rememberSaveable { mutableStateOf(false) }
        var showBottomSheetEdit by rememberSaveable { mutableStateOf(false) }
        var editingCredential by remember { mutableStateOf<UserCredentials?>(null) }
        var accountname by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var showDeleteConfirmation by remember { mutableStateOf(false) }

        val builder = Room.databaseBuilder(
            appContext,
            UserCredentialsDatabase::class.java,
            "user_credentials.db"
        )


        val cipherFactory = SupportFactory(SQLiteDatabase.getBytes("Passphrase".toCharArray()))
        builder.openHelperFactory(cipherFactory)
        builder.build()
        val repository by lazy { UserCredentialsRepository(builder.build().userCredentialsDao()) }
        val factory = remember { CredentialsViewModelFactory(repository) }
        val viewModel: CredentialsViewModel = viewModel(factory = factory)


        PasswordManagerTheme {


            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(onClick = { showBottomSheet = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.Blue)

                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TopAppBar()
                    Spacer(modifier = Modifier.height(16.dp))
                    UserCredentialsList(viewModel = viewModel, onEditClick = { credential ->
                        editingCredential = credential
                        accountname = credential.sitename
                        username = credential.username
                        password = credential.password
                        showBottomSheetEdit = true
                    })
                }

                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showBottomSheet = false
                            // editingCredential = null
                            accountname = ""
                            username = ""
                            password = ""
                            // showDeleteConfirmation = false
                        }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column {
                                TextField(
                                    value = accountname,
                                    onValueChange = { accountname = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Account Name") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                TextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Username / Email") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                TextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Password") }

                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {


                                        viewModel.addCredentials(accountname, username, password)

                                        accountname = ""
                                        username = ""
                                        password = ""
                                        showBottomSheet = false
                                        editingCredential = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .fillMaxWidth()

                                ) {
                                    Text("Add New Account")
                                }

                            }
                        }
                    }
                }

                if (showBottomSheetEdit) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showBottomSheetEdit = false
                            editingCredential = null
                            accountname = ""
                            username = ""
                            password = ""
                            showDeleteConfirmation = false
                        }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column {
                                TextField(
                                    value = if (showDeleteConfirmation) "" else accountname,
                                    onValueChange = { accountname = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Account Name") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                TextField(
                                    value = if (showDeleteConfirmation) "" else username,
                                    onValueChange = { username = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Username / Email") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                TextField(
                                    value = if (showDeleteConfirmation) "" else password,
                                    onValueChange = { password = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Password") }
                                )



                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {

                                    Button(
                                        onClick = {
                                            if (editingCredential != null) {

                                                viewModel.updateCredentials(
                                                    accountname,
                                                    username,
                                                    password
                                                )
                                            } else {
                                                viewModel.addCredentials(
                                                    accountname,
                                                    username,
                                                    password
                                                )
                                            }

                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RectangleShape
                                    ) {
                                        Text("Save Changes")
                                    }

                                    if (editingCredential != null) {
                                        // Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.deleteCredentials(
                                                    UserCredentials(
                                                        accountname,
                                                        username,
                                                        password
                                                    )
                                                )
                                                showDeleteConfirmation = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color.Red
                                            )
                                        ) {
                                            Text("Delete Account")
                                        }
                                    }


                                }


                            }
                        }
                    }

                }
            }
        }
    }

    @Composable
    fun UserCredentialsList(
        viewModel: CredentialsViewModel,
        onEditClick: (UserCredentials) -> Unit
    ) {
        val credentials by viewModel.allCredentials.collectAsStateWithLifecycle(emptyList())

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(credentials) { credential ->
                CredentialCard(
                    credential,
                    onDelete = { viewModel.deleteCredentials(it) },
                    onEditClick = onEditClick
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CredentialCard(
        credential: UserCredentials,
        onDelete: (UserCredentials) -> Unit,
        onEditClick: (UserCredentials) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onEditClick(credential) },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Text(
                        text = credential.sitename.replaceFirstChar(Char::titlecase),
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "  *******")
                    // Text(text = "Password: ${"*".repeat(credential.password.length)}")
                }
                /*  IconButton(onClick = { onDelete(credential) }) {
                  Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
              }

             */
            }
        }
    }


    @Composable
    fun ErrorDisplay() {

        val context = LocalContext.current
        var showDialog by remember { mutableStateOf(true) }

        if (showDialog){
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("PasswordManager is locked") },
                text = { Text("For your security, you can only use Password Manager when it's unlocked") },
                confirmButton = {
                    Button(onClick = {
                        promptManager.showBiometricPrompt(
                            title = "Unlock Password Manager",
                            description = "Use your PIN or fingerprint to unlock Password Manager"
                        )
                    }) {
                        Text("Unlock")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showDialog = false
                        (context as Activity).finish()
                    }) {
                        Text("Cancel")
                    }
                }
            )
    }
    }
}


/*
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PasswordManagerTheme {

    }
}*/
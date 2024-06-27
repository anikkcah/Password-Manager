package com.example.passwordmanager

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.passwordmanager.db.UserCredentials
import com.example.passwordmanager.db.repository.UserCredentialsRepository
import com.example.passwordmanager.viewmodel.CredentialsViewModel
import com.example.passwordmanager.viewmodel.CredentialsViewModelFactory
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {


            MyApp(appContext = applicationContext)
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp(appContext: Context) {


    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showBottomSheetEdit by rememberSaveable { mutableStateOf(false) }
    var editingCredential by remember { mutableStateOf<UserCredentials?>(null) }
    var accountname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val builder = Room.databaseBuilder(appContext, UserCredentialsDatabase::class.java, "user_credentials.db")


    val cipherFactory = SupportFactory(SQLiteDatabase.getBytes("Passphrase".toCharArray()))
    builder.openHelperFactory(cipherFactory)
    builder.build()
    val repository by lazy { UserCredentialsRepository(builder.build().userCredentialsDao()) }
    val factory = remember { CredentialsViewModelFactory(repository) }
    val viewModel: CredentialsViewModel = viewModel(factory = factory)



    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Filled.Add,contentDescription = "Add", tint=Color.Blue)

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
                                value = if(showDeleteConfirmation) "" else password,
                                onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Password") }
                            )



                        Spacer(modifier = Modifier.height(16.dp))

                        Row() {

                            Button(
                                onClick = {
                                    if (editingCredential != null) {

                                        viewModel.updateCredentials(
                                            accountname,
                                            username,
                                            password
                                        )
                                    } else {
                                        viewModel.addCredentials(accountname, username, password)
                                    }

                                },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text("Save Changes")
                            }

                            if (editingCredential != null) {
                                Spacer(modifier = Modifier.height(8.dp))
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
                                    modifier = Modifier.align(Alignment.Bottom),
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

@Composable
fun UserCredentialsList(viewModel: CredentialsViewModel, onEditClick: (UserCredentials) -> Unit) {
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
                Text(text = credential.sitename.replaceFirstChar(Char::titlecase), fontWeight = FontWeight.Bold)
                Text(text =  "  *******")
                // Text(text = "Password: ${"*".repeat(credential.password.length)}")
            }
            /*  IconButton(onClick = { onDelete(credential) }) {
                  Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
              }

             */
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(){
    androidx.compose.material3.TopAppBar(title = { Text(text = "Password Manager") })
}

/*
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PasswordManagerTheme {

    }
}*/
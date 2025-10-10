package com.example.gamezone.ui.screen

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.io.File

fun createImageUri(context: Context): Uri {
    val imageFile = File(context.cacheDir, "images/profile_pic_${System.currentTimeMillis()}.jpg").apply {
        parentFile?.mkdirs()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RegistrationScreen(
    onRegistrationSuccess: () -> Unit,
    authViewModel: AuthViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val username by authViewModel.username.collectAsState()
    val email by authViewModel.email.collectAsState()
    val password by authViewModel.password.collectAsState()
    val fullName by authViewModel.fullName.collectAsState()
    val phone by authViewModel.phone.collectAsState()
    val imageUri by authViewModel.imageUri.collectAsState()
    val registrationState by authViewModel.registrationState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(registrationState) {
        if (registrationState is RegistrationState.Success) {
            Log.d("RegistrationScreen", "Registracija uspešna – prelazim dalje")
            onRegistrationSuccess()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) authViewModel.onImageUriChange(uri)
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                val localUri = tempImageUri
                if (localUri != null) {
                    authViewModel.onImageUriChange(localUri)
                }
            }
        }
    )

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        listOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val permissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

    LaunchedEffect(Unit) {
        authViewModel.resetRegistrationStateToIdle()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Registracija za GameZone", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { showImageSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUri),
                        contentDescription = "Profilna slika",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_camera),
                            contentDescription = "Dodaj sliku",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Dodaj sliku", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Text(
                text = if (imageUri != null) "Promeni sliku" else "Klikni za dodavanje slike",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { authViewModel.onFullNameChange(it) },
                label = { Text("Ime i prezime") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { authViewModel.onUsernameChange(it) },
                label = { Text("Korisničko ime") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { authViewModel.onEmailChange(it) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { authViewModel.onPhoneChange(it) },
                label = { Text("Broj telefona") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { authViewModel.onPasswordChange(it) },
                label = { Text("Šifra") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        when {
                            username.isBlank() -> snackbarHostState.showSnackbar("Unesite korisničko ime")
                            fullName.isBlank() -> snackbarHostState.showSnackbar("Unesite ime i prezime")
                            email.isBlank() -> snackbarHostState.showSnackbar("Unesite email")
                            phone.isBlank() -> snackbarHostState.showSnackbar("Unesite broj telefona")
                            password.length < 6 -> snackbarHostState.showSnackbar("Šifra mora imati najmanje 6 karaktera")
                            else -> authViewModel.registerUser()
                        }
                    }
                },
                enabled = registrationState !is RegistrationState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (registrationState is RegistrationState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Registruj se")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (registrationState is RegistrationState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = (registrationState as RegistrationState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Izaberi izvor slike") },
            text = { Text("Odakle želite da dodate sliku?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {

                        permissionsState.launchMultiplePermissionRequest()
                        val cameraPermission = permissionsState.permissions.find { it.permission == Manifest.permission.CAMERA }


                        if (cameraPermission?.status?.isGranted == true) {

                            val uri = createImageUri(context)
                            tempImageUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            snackbarHostState.showSnackbar("Dozvola za kameru je odbijena.")
                        }
                    }
                    showImageSourceDialog = false
                }) {
                    Text("Kamera")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {

                        permissionsState.launchMultiplePermissionRequest()
                        val storagePermissionGranted =
                            permissionsState.permissions.filterNot { it.permission == Manifest.permission.CAMERA }
                                .all { it.status.isGranted }


                        if (storagePermissionGranted) {
                            galleryLauncher.launch("image/*")
                        } else {
                            snackbarHostState.showSnackbar("Dozvola za galeriju je odbijena.")
                        }
                    }
                    showImageSourceDialog = false
                }) {
                    Text("Galerija")
                }
            }
        )
    }
}

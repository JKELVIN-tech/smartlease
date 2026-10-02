package com.smartlease.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(vm: AppViewModel) {
    var signUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(Col.background).statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 32.dp)
    ) {
        Box(Modifier.size(36.dp).background(Col.ink, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Text("S", color = Col.background, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        Eyebrow("SmartLease", Modifier.padding(top = 32.dp))
        Text(
            if (signUp) "Open your ledger" else "Welcome back",
            Modifier.padding(top = 4.dp, bottom = 32.dp), color = Col.ink,
            fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 36.sp
        )
        Field("Email") { Input(email, { email = it }, keyboard = KeyboardType.Email) }
        Field("Password") { Input(password, { password = it }, keyboard = KeyboardType.Password, password = true) }
        SubmitButton(if (signUp) "Create account" else "Sign in", vm.busy, busyText = "Please wait…") {
            if (email.isBlank() || password.length < 6) vm.message = "Enter your email and a password of at least 6 characters."
            else vm.authenticate(email.trim(), password, signUp)
        }
        Text(
            if (signUp) "Have an account? Sign in" else "New here? Create an account",
            Modifier.fillMaxWidth().clickable { signUp = !signUp }.padding(top = 20.dp, bottom = 8.dp),
            color = Col.mutedFg, fontSize = 13.sp, textAlign = TextAlign.Center
        )
    }
}

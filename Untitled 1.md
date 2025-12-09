

```kotlin

// ============================================
// REGISTRATION SCREEN - JETPACK COMPOSE
// Complete implementation with explanations
// ============================================

// STEP 1: Data Classes & State Management
// ========================================

data class RegistrationFormState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    val showConfirmPassword: Boolean = false,
    val isLoading: Boolean = false
)

data class FormErrors(
    val email: String? = null,
    val username: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null
)

// ============================================
// STEP 2: MAIN SCREEN COMPOSABLE
// ============================================

@Composable
fun RegistrationScreen(
    onSignUpSuccess: () -> Unit = {},
    onSignInClick: () -> Unit = {}
) {
    var formState by remember { mutableStateOf(RegistrationFormState()) }
    var errors by remember { mutableStateOf(FormErrors()) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    // Full screen with keyboard padding
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()  // 🔑 KEY: Prevents keyboard overlap
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF0F8F5),
                        Color.White
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(24.dp)
    ) {
        // HEADER SECTION
        item { RegistrationHeader() }
        
        // FORM SECTION
        item {
            Spacer(Modifier.height(32.dp))
        }
        
        // Email Field
        item {
            EmailTextField(
                value = formState.email,
                error = errors.email,
                onValueChange = { newValue ->
                    formState = formState.copy(email = newValue)
                    errors = errors.copy(email = null)  // Clear error on change
                },
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
        }
        
        item { Spacer(Modifier.height(12.dp)) }
        
        // Username Field
        item {
            UsernameTextField(
                value = formState.username,
                error = errors.username,
                onValueChange = { newValue ->
                    formState = formState.copy(username = newValue)
                    errors = errors.copy(username = null)
                },
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
        }
        
        item { Spacer(Modifier.height(12.dp)) }
        
        // Password Field
        item {
            PasswordTextField(
                value = formState.password,
                isPasswordVisible = formState.showPassword,
                error = errors.password,
                onValueChange = { newValue ->
                    formState = formState.copy(password = newValue)
                    errors = errors.copy(password = null)
                },
                onVisibilityToggle = {
                    formState = formState.copy(showPassword = !formState.showPassword)
                },
                label = "Password",
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
        }
        
        item { Spacer(Modifier.height(12.dp)) }
        
        // Confirm Password Field
        item {
            PasswordTextField(
                value = formState.confirmPassword,
                isPasswordVisible = formState.showConfirmPassword,
                error = errors.confirmPassword,
                onValueChange = { newValue ->
                    formState = formState.copy(confirmPassword = newValue)
                    errors = errors.copy(confirmPassword = null)
                },
                onVisibilityToggle = {
                    formState = formState.copy(showConfirmPassword = !formState.showConfirmPassword)
                },
                label = "Confirm password",
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                )
            )
        }
        
        item { Spacer(Modifier.height(24.dp)) }
        
        // Sign Up Button
        item {
            SignUpButton(
                isLoading = formState.isLoading,
                onClick = {
                    val validationErrors = validateForm(formState)
                    if (validationErrors.email == null &&
                        validationErrors.username == null &&
                        validationErrors.password == null &&
                        validationErrors.confirmPassword == null) {
                        
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        // Perform signup
                        formState = formState.copy(isLoading = true)
                        // Simulate network call
                        onSignUpSuccess()
                    } else {
                        errors = validationErrors
                    }
                }
            )
        }
        
        item { Spacer(Modifier.height(24.dp)) }
        
        // Divider with "or"
        item {
            DividerWithText(text = "or")
        }
        
        item { Spacer(Modifier.height(24.dp)) }
        
        // Social Login Buttons
        item {
            SocialLoginButtons(
                onFacebookClick = { /* Navigate to Facebook login */ },
                onAppleClick = { /* Navigate to Apple login */ },
                onGoogleClick = { /* Navigate to Google login */ }
            )
        }
        
        item { Spacer(Modifier.height(32.dp)) }
        
        // Sign In Link
        item {
            SignInPrompt(onSignInClick = onSignInClick)
        }
        
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ============================================
// STEP 3: INDIVIDUAL COMPONENT COMPOSABLES
// ============================================

// Header Section
@Composable
fun RegistrationHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Lotus Icon
        Icon(
            painter = painterResource(id = R.drawable.ic_lotus),
            contentDescription = "Lotus Icon",
            modifier = Modifier.size(50.dp),
            tint = Color(0xFF4E6C50)
        )
        
        Spacer(Modifier.height(20.dp))
        
        // Title
        Text(
            text = "Your journey starts here",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3F4B3B),
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(8.dp))
        
        // Subtitle
        Text(
            text = "Take the first step",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF3F4B3B),
            textAlign = TextAlign.Center
        )
    }
}

// Email TextField
@Composable
fun EmailTextField(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    keyboardActions: KeyboardActions = KeyboardActions()
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = { Text("E-mail") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email Icon",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF999999)
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                autoCorrect = false,
                capitalization = KeyboardCapitalization.None
            ),
            keyboardActions = keyboardActions,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFE8E8E8),
                focusedContainerColor = Color(0xFFE8E8E8),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color(0xFF4E6C50),
                unfocusedPlaceholderColor = Color(0xFF999999),
                focusedPlaceholderColor = Color(0xFF666666)
            ),
            isError = error != null
        )
        
        // Error message
        if (error != null) {
            Text(
                text = error,
                color = Color(0xFFB00020),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 12.dp)
            )
        }
    }
}

// Username TextField
@Composable
fun UsernameTextField(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    keyboardActions: KeyboardActions = KeyboardActions()
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = { Text("Username") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Username Icon",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF999999)
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.None
            ),
            keyboardActions = keyboardActions,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFE8E8E8),
                focusedContainerColor = Color(0xFFE8E8E8),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color(0xFF4E6C50),
                unfocusedPlaceholderColor = Color(0xFF999999),
                focusedPlaceholderColor = Color(0xFF666666)
            ),
            isError = error != null
        )
        
        if (error != null) {
            Text(
                text = error,
                color = Color(0xFFB00020),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 12.dp)
            )
        }
    }
}

// Password TextField (Reusable for both password and confirm password)
@Composable
fun PasswordTextField(
    value: String,
    isPasswordVisible: Boolean,
    error: String?,
    onValueChange: (String) -> Unit,
    onVisibilityToggle: () -> Unit,
    label: String = "Password",
    keyboardActions: KeyboardActions = KeyboardActions()
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password Icon",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF999999)
                )
            },
            trailingIcon = {
                IconButton(onClick = onVisibilityToggle) {
                    Icon(
                        imageVector = if (isPasswordVisible) 
                            Icons.Default.VisibilityOff 
                        else 
                            Icons.Default.Visibility,
                        contentDescription = "Toggle Password Visibility",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF999999)
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) 
                VisualTransformation.None 
            else 
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                autoCorrect = false,
                capitalization = KeyboardCapitalization.None
            ),
            keyboardActions = keyboardActions,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFE8E8E8),
                focusedContainerColor = Color(0xFFE8E8E8),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color(0xFF4E6C50),
                unfocusedPlaceholderColor = Color(0xFF999999),
                focusedPlaceholderColor = Color(0xFF666666)
            ),
            isError = error != null
        )
        
        if (error != null) {
            Text(
                text = error,
                color = Color(0xFFB00020),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 12.dp)
            )
        }
    }
}

// Sign Up Button
@Composable
fun SignUpButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp),
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4E6C50),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF7A8F7E)
        ),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Sign up",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// Divider with Text
@Composable
fun DividerWithText(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
            color = Color(0xFFCCCCCC)
        )
        
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF999999),
            fontWeight = FontWeight.Normal
        )
        
        Divider(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
            color = Color(0xFFCCCCCC)
        )
    }
}

// Social Login Buttons
@Composable
fun SocialLoginButtons(
    onFacebookClick: () -> Unit,
    onAppleClick: () -> Unit,
    onGoogleClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Facebook Button
        SocialButton(
            icon = R.drawable.ic_facebook,
            contentDescription = "Facebook",
            onClick = onFacebookClick
        )
        
        Spacer(Modifier.width(24.dp))
        
        // Apple Button
        SocialButton(
            icon = R.drawable.ic_apple,
            contentDescription = "Apple",
            onClick = onAppleClick
        )
        
        Spacer(Modifier.width(24.dp))
        
        // Google Button
        SocialButton(
            icon = R.drawable.ic_google,
            contentDescription = "Google",
            onClick = onGoogleClick
        )
    }
}

@Composable
fun SocialButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFF5F5F5),
        modifier = Modifier
            .size(48.dp)
            .clickable(
                indication = ripple(bounded = false),
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(32.dp)
                .padding(8.dp)
        )
    }
}

// Sign In Prompt
@Composable
fun SignInPrompt(onSignInClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Already have an account? ",
            fontSize = 14.sp,
            color = Color(0xFF999999)
        )
        
        TextButton(onClick = onSignInClick) {
            Text(
                text = "Sign in",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E6C50)
            )
        }
    }
}

// ============================================
// STEP 4: VALIDATION LOGIC
// ============================================

fun validateForm(formState: RegistrationFormState): FormErrors {
    val errors = FormErrors()
    
    // Email validation
    val emailError = when {
        formState.email.isEmpty() -> "Email cannot be empty"
        !formState.email.contains("@") -> "Invalid email format"
        !formState.email.contains(".") -> "Invalid email domain"
        else -> null
    }
    
    // Username validation
    val usernameError = when {
        formState.username.isEmpty() -> "Username cannot be empty"
        formState.username.length < 3 -> "Username must be at least 3 characters"
        formState.username.length > 20 -> "Username cannot exceed 20 characters"
        else -> null
    }
    
    // Password validation
    val passwordError = when {
        formState.password.isEmpty() -> "Password cannot be empty"
        formState.password.length < 8 -> "Password must be at least 8 characters"
        !formState.password.any { it.isUpperCase() } -> "Password must contain uppercase letter"
        !formState.password.any { it.isDigit() } -> "Password must contain number"
        else -> null
    }
    
    // Confirm password validation
    val confirmPasswordError = when {
        formState.confirmPassword.isEmpty() -> "Please confirm password"
        formState.password != formState.confirmPassword -> "Passwords do not match"
        else -> null
    }
    
    return FormErrors(
        email = emailError,
        username = usernameError,
        password = passwordError,
        confirmPassword = confirmPasswordError
    )
}

// ============================================
// STEP 5: PREVIEW
// ============================================

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
fun RegistrationScreenPreview() {
    AppTheme {
        RegistrationScreen()
    }
}

```


# 🎨 Complete Guide: Designing Registration UI in Jetpack Compose

## Learn by Building This Beautiful Auth Screen

---

# 📱 UI BREAKDOWN

This registration screen has 6 main sections:

```
┌─────────────────────────────────┐
│  1. HEADER (Logo + Title)       │
├─────────────────────────────────┤
│  2. EMAIL FIELD                 │
│  3. USERNAME FIELD              │
│  4. PASSWORD FIELD              │
│  5. CONFIRM PASSWORD FIELD      │
│  6. SIGN UP BUTTON              │
├─────────────────────────────────┤
│  7. DIVIDER with "or"           │
│  8. SOCIAL LOGIN (3 buttons)    │
├─────────────────────────────────┤
│  9. SIGN IN LINK                │
└─────────────────────────────────┘
```

---

# 🎯 KEY CONCEPTS YOU'LL LEARN

## 1. LazyColumn with `imePadding()` (Keyboard Handling)

```kotlin
// ✅ CORRECT: Keyboard doesn't cover content
LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .imePadding()  // 🔑 KEY: Auto-adds padding when keyboard appears
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFF0F8F5), Color.White)
            )
        ),
    horizontalAlignment = Alignment.CenterHorizontally,
    contentPadding = PaddingValues(24.dp)
)

// Why imePadding()?
// - When keyboard appears, automatically adds bottom padding
// - Content scrolls into view
// - Removes padding when keyboard closes
// - No manual management needed!
```

---

## 2. Proper TextField Styling

```kotlin
// ❌ WRONG - Looks flat and unpolished
OutlinedTextField(
    value = email,
    onValueChange = { email = it },
    modifier = Modifier.fillMaxWidth()
)

// ✅ CORRECT - Modern, polished design
OutlinedTextField(
    value = email,
    onValueChange = { email = it },
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),  // Fixed height for consistency
    placeholder = { Text("E-mail") },
    leadingIcon = {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = "Email Icon",
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF999999)
        )
    },
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Email,  // Email keyboard
        imeAction = ImeAction.Next           // Next button
    ),
    singleLine = true,
    shape = RoundedCornerShape(12.dp),  // 🔑 Rounded corners
    colors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = Color(0xFFE8E8E8),  // Filled background
        focusedContainerColor = Color(0xFFE8E8E8),
        unfocusedBorderColor = Color.Transparent,     // No outline when unfocused
        focusedBorderColor = Color(0xFF4E6C50),       // Green outline when focused
        unfocusedPlaceholderColor = Color(0xFF999999),
        focusedPlaceholderColor = Color(0xFF666666)
    )
)
```

### Why These Styling Choices?

|Property|Value|Why?|
|---|---|---|
|`height(56.dp)`|Fixed size|Consistent touch target (48dp minimum)|
|`shape = RoundedCornerShape(12.dp)`|Rounded|Modern design trend|
|`containerColor = Color(0xFFE8E8E8)`|Light gray|Subtle contrast|
|`unfocusedBorderColor = Transparent`|No visible border|Clean design|
|`focusedBorderColor = Color(0xFF4E6C50)`|Green|Brand color, indicates focus|

---

## 3. Password Visibility Toggle

```kotlin
// ✅ Password field with eye icon
OutlinedTextField(
    value = password,
    onValueChange = { password = it },
    visualTransformation = if (isPasswordVisible) 
        VisualTransformation.None              // Show password
    else 
        PasswordVisualTransformation(),         // Mask password (•••••)
    
    trailingIcon = {
        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
            Icon(
                imageVector = if (isPasswordVisible) 
                    Icons.Default.VisibilityOff 
                else 
                    Icons.Default.Visibility,
                contentDescription = "Toggle Password"
            )
        }
    }
)

// How it works:
// 1. User clicks eye icon
// 2. isPasswordVisible state toggles
// 3. visualTransformation changes
// 4. Text shows/hides automatically
```

---

## 4. Form State Management (Advanced)

```kotlin
// ✅ CORRECT: Centralized state object
data class RegistrationFormState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    val showConfirmPassword: Boolean = false,
    val isLoading: Boolean = false
)

@Composable
fun RegistrationScreen() {
    var formState by remember { mutableStateOf(RegistrationFormState()) }
    var errors by remember { mutableStateOf(FormErrors()) }
    
    // Update field
    EmailTextField(
        value = formState.email,
        onValueChange = { newValue ->
            formState = formState.copy(email = newValue)  // ✅ Immutable update
            errors = errors.copy(email = null)            // ✅ Clear error on change
        }
    )
}

// Why use data class?
// 1. Single source of truth
// 2. Easy to pass to functions
// 3. Immutable updates prevent bugs
// 4. Clear field relationships
```

---

## 5. Keyboard Navigation Between Fields

```kotlin
// ✅ CORRECT: Navigate between fields smoothly
val keyboardController = LocalSoftwareKeyboardController.current
val focusManager = LocalFocusManager.current

// Email field
EmailTextField(
    keyboardActions = KeyboardActions(
        onNext = { focusManager.moveFocus(FocusDirection.Down) }
        //      ↑ Move to next field (username)
    ),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
)

// Password field (last field)
PasswordTextField(
    keyboardActions = KeyboardActions(
        onDone = {
            keyboardController?.hide()  // Hide keyboard
            focusManager.clearFocus()   // Remove focus from all fields
        }
    ),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
)

// User experience flow:
// 1. Type email → Press "Next" → Jump to username
// 2. Type username → Press "Next" → Jump to password
// 3. Type password → Press "Done" → Keyboard disappears
```

---

## 6. Error Display & Validation

```kotlin
// ✅ CORRECT: Real-time validation feedback
Column(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = email,
        isError = error != null,  // 🔑 Visual error indicator
        // ... other properties
    )
    
    // Show error message
    if (error != null) {
        Text(
            text = error,
            color = Color(0xFFB00020),  // Red color
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, start = 12.dp)
        )
    }
}

// Validation function
fun validateForm(formState: RegistrationFormState): FormErrors {
    return FormErrors(
        email = when {
            formState.email.isEmpty() -> "Email cannot be empty"
            !formState.email.contains("@") -> "Invalid email format"
            else -> null
        },
        password = when {
            formState.password.isEmpty() -> "Password cannot be empty"
            formState.password.length < 8 -> "Password must be 8+ characters"
            !formState.password.any { it.isDigit() } -> "Must contain number"
            !formState.password.any { it.isUpperCase() } -> "Must contain uppercase"
            else -> null
        }
    )
}

// Show error on sign up
Button(onClick = {
    val validationErrors = validateForm(formState)
    if (validationErrors.email == null && validationErrors.password == null) {
        // Proceed with signup
    } else {
        errors = validationErrors  // Show errors
    }
})
```

---

## 7. Composable Reusability

```kotlin
// ❌ DON'T: Repeat code for password & confirm password
OutlinedTextField(
    value = password,
    onValueChange = { password = it },
    // ... 20 lines of styling
)

OutlinedTextField(
    value = confirmPassword,
    onValueChange = { confirmPassword = it },
    // ... same 20 lines of styling again!
)

// ✅ DO: Extract reusable component
@Composable
fun PasswordTextField(
    value: String,
    isPasswordVisible: Boolean,
    label: String = "Password",
    onValueChange: (String) -> Unit,
    onVisibilityToggle: () -> Unit,
    keyboardActions: KeyboardActions = KeyboardActions()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label) },
        visualTransformation = if (isPasswordVisible) 
            VisualTransformation.None 
        else 
            PasswordVisualTransformation(),
        // ... styling code (written once!)
    )
}

// Usage (reusable):
PasswordTextField(
    value = formState.password,
    isPasswordVisible = formState.showPassword,
    label = "Password",
    onValueChange = { formState = formState.copy(password = it) },
    onVisibilityToggle = { formState = formState.copy(showPassword = !formState.showPassword) }
)

PasswordTextField(
    value = formState.confirmPassword,
    isPasswordVisible = formState.showConfirmPassword,
    label = "Confirm password",
    onValueChange = { formState = formState.copy(confirmPassword = it) },
    onVisibilityToggle = { formState = formState.copy(showConfirmPassword = !formState.showConfirmPassword) }
)
```

---

## 8. Gradient Background

```kotlin
// ✅ Modern gradient effect
LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF0F8F5),  // Light green at top
                    Color.White         // White at bottom
                )
            )
        )
)

// Brush options:
// - Brush.verticalGradient() → Top to bottom
// - Brush.horizontalGradient() → Left to right
// - Brush.radialGradient() → Circular gradient
// - Brush.sweepGradient() → Rotating gradient
```

---

## 9. Divider with Text

```kotlin
// ✅ Modern design pattern
Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(30.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    // Left line takes 1/3 of space
    Divider(
        modifier = Modifier
            .weight(1f)
            .height(1.dp),
        color = Color(0xFFCCCCCC)
    )
    
    // Text in center
    Text(
        text = "or",
        fontSize = 14.sp,
        color = Color(0xFF999999)
    )
    
    // Right line takes 1/3 of space
    Divider(
        modifier = Modifier
            .weight(1f)
            .height(1.dp),
        color = Color(0xFFCCCCCC)
    )
}

// Why weight(1f)?
// - Both dividers take remaining space equally
// - Text stays centered automatically
```

---

## 10. Social Login Buttons

```kotlin
// ✅ Reusable social button component
@Composable
fun SocialButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,  // 🔑 Circle shape
        color = Color(0xFFF5F5F5),  // Light gray background
        modifier = Modifier
            .size(48.dp)
            .clickable(
                indication = ripple(bounded = false),  // Ripple effect
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// Usage in Row
Row(
    horizontalArrangement = Arrangement.Center
) {
    SocialButton(
        icon = R.drawable.ic_facebook,
        contentDescription = "Facebook"
    ) { /* Navigate to Facebook */ }
    
    Spacer(Modifier.width(24.dp))
    
    SocialButton(
        icon = R.drawable.ic_google,
        contentDescription = "Google"
    ) { /* Navigate to Google */ }
}
```

---

# 📋 STEP-BY-STEP IMPLEMENTATION GUIDE

## Step 1: Set Up Project Dependencies

```gradle
dependencies {
    // Core Compose
    implementation 'androidx.compose.ui:ui:1.6.0'
    implementation 'androidx.compose.material3:material3:1.1.1'
    
    // Material Icons
    implementation 'androidx.compose.material:material-icons-extended:1.6.0'
    
    // ViewModel & State
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1'
    implementation 'androidx.lifecycle:lifecycle-runtime-compose:2.6.1'
    
    // Navigation
    implementation 'androidx.navigation:navigation-compose:2.7.0'
}
```

---

## Step 2: Create Data Classes

```kotlin
// In your project, create a new file: data/RegistrationFormState.kt

data class RegistrationFormState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    val showConfirmPassword: Boolean = false,
    val isLoading: Boolean = false
)

data class FormErrors(
    val email: String? = null,
    val username: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null
)
```

---

## Step 3: Build the Main Screen

```kotlin
@Composable
fun RegistrationScreen(
    onSignUpSuccess: () -> Unit = {},
    onSignInClick: () -> Unit = {}
) {
    var formState by remember { mutableStateOf(RegistrationFormState()) }
    var errors by remember { mutableStateOf(FormErrors()) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF0F8F5),
                        Color.White
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(24.dp)
    ) {
        // ... Rest of implementation
    }
}
```

---

## Step 4: Build Individual Components

Create separate files for each component:

- `screens/RegistrationHeader.kt`
- `components/EmailTextField.kt`
- `components/PasswordTextField.kt`
- `components/SignUpButton.kt`
- `components/SocialLoginButtons.kt`

---

# 🎓 LEARNING OUTCOMES

After completing this tutorial, you'll understand:

✅ **Keyboard Management**

- `imePadding()` for keyboard avoidance
- `KeyboardOptions` and `ImeAction`
- `LocalSoftwareKeyboardController` for manual control
- `FocusManager` for navigation

✅ **TextField Styling**

- Custom colors and shapes
- Icons (leading/trailing)
- Visual transformation (password masking)
- Error states

✅ **Form State**

- Centralized state management
- Immutable updates with `copy()`
- Real-time validation
- Error display

✅ **Component Reusability**

- Extracting common components
- Passing lambdas as callbacks
- Default parameters

✅ **Modern UI Patterns**

- Gradient backgrounds
- Rounded corners
- Ripple effects
- Social login buttons

✅ **Best Practices**

- Accessibility (touch targets, icons)
- Responsive layout (fillMaxWidth)
- Error handling
- Loading states

---

# 🚀 NEXT STEPS

1. **Build the UI** - Copy the Compose code and run it
2. **Customize Colors** - Change brand colors to match your app
3. **Add API Integration** - Connect to backend for real signup
4. **Implement Navigation** - Route to home screen after signup
5. **Add Analytics** - Track user registration events
6. **Test Edge Cases** - Try different validation scenarios

**Remember:** Start simple, then add complexity. Get the basic layout working before optimizing!
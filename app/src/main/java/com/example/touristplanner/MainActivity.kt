package com.example.touristplanner

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.UUID

// =====================================================
// 1. PROFESSIONAL THEME COLORS
// =====================================================
val PrimaryBlue = Color(0xFF1E3A8A)
val AccentOrange = Color(0xFFF59E0B)
val BgLight = Color(0xFFF8FAFC)
val TextDark = Color(0xFF1E293B)
val TextGray = Color(0xFF64748B)

val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = AccentOrange,
    background = BgLight,
    surface = Color.White,
    onPrimary = Color.White,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFF1F5F9)
)

// =====================================================
// 2. OOP MODEL LAYER (ABSTRACTION & INHERITANCE)
// =====================================================

abstract class Destination(
    val id: Int, val name: String, val location: String, open val baseCost: Double,
    val rating: Double, val imageResId: Int,
    val description: String, val latitude: Double, val longitude: Double, val weather: String
) {
    open fun category(): String = "General"
    open fun icon(): ImageVector = Icons.Default.Place
    open fun costForTravelers(count: Int): Double = baseCost * count
}

class Beach(id: Int, name: String, location: String, baseCost: Double, rating: Double, imageResId: Int, description: String, lat: Double, lng: Double, weather: String, val hasResort: Boolean) : Destination(id, name, location, baseCost, rating, imageResId, description, lat, lng, weather) {
    override fun category() = "Beach"
    override fun icon() = Icons.Default.BeachAccess
}

class HistoricalSite(id: Int, name: String, location: String, baseCost: Double, rating: Double, imageResId: Int, description: String, lat: Double, lng: Double, weather: String, val entryFee: Double) : Destination(id, name, location, baseCost, rating, imageResId, description, lat, lng, weather) {
    override val baseCost: Double get() = super.baseCost + entryFee
    override fun category() = "Historical"
    override fun icon() = Icons.Default.AccountBalance
}

class AdventureSpot(id: Int, name: String, location: String, baseCost: Double, rating: Double, imageResId: Int, description: String, lat: Double, lng: Double, weather: String, val difficulty: String) : Destination(id, name, location, baseCost, rating, imageResId, description, lat, lng, weather) {
    override fun category() = "Adventure"
    override fun icon() = Icons.Default.Terrain
}

class Restaurant(id: Int, name: String, location: String, baseCost: Double, rating: Double, imageResId: Int, description: String, lat: Double, lng: Double, weather: String, val cuisine: String) : Destination(id, name, location, baseCost, rating, imageResId, description, lat, lng, weather) {
    override fun category() = "Food"
    override fun icon() = Icons.Default.Restaurant
}

data class Vehicle(
    val id: Int,
    val name: String,
    val type: String,
    val baseFare: Double,
    val icon: ImageVector,
    val driverName: String,
    val driverPhone: String,
    val plateNumber: String
)

enum class TicketStatus { ACTIVE, CANCELLED }

data class TripTicket(
    val ticketId: String,
    val bookingDate: Date,
    val destinations: List<Destination>,
    val vehicle: Vehicle?,
    val travelers: Int,
    val totalPaid: Double,
    val paymentMethod: String,
    val startTime: String?,
    var status: TicketStatus = TicketStatus.ACTIVE
)

class BookingSystem(val ownerName: String, val ownerEmail: String) {
    val cartDestinations = mutableStateListOf<Destination>()
    val favoriteDestinations = mutableStateListOf<Destination>()
    val confirmedTickets = mutableStateListOf<TripTicket>()

    var selectedVehicle by mutableStateOf<Vehicle?>(null)
    var travelers by mutableIntStateOf(1)
    var startDate by mutableStateOf<Date?>(null)
    var endDate by mutableStateOf<Date?>(null)
    var startTime by mutableStateOf<String?>(null)

    fun toggleCart(dest: Destination) {
        if (cartDestinations.any { it.id == dest.id }) cartDestinations.removeAll { it.id == dest.id }
        else cartDestinations.add(dest)
    }

    fun toggleFavorite(dest: Destination) {
        if (favoriteDestinations.any { it.id == dest.id }) favoriteDestinations.removeAll { it.id == dest.id }
        else favoriteDestinations.add(dest)
    }

    fun calculateSubtotal(): Double = cartDestinations.sumOf { it.costForTravelers(travelers) }
    fun calculateTransport(): Double = selectedVehicle?.baseFare ?: 0.0
    fun calculateTotal(): Double = calculateSubtotal() + calculateTransport()

    fun confirmBooking(paymentMethod: String) {
        val ticket = TripTicket(
            ticketId = UUID.randomUUID().toString().take(8).uppercase(),
            bookingDate = Date(),
            destinations = cartDestinations.toList(),
            vehicle = selectedVehicle,
            travelers = travelers,
            totalPaid = calculateTotal(),
            paymentMethod = paymentMethod,
            startTime = startTime
        )
        confirmedTickets.add(ticket)
        cartDestinations.clear()
        selectedVehicle = null
        travelers = 1
        startTime = null
    }

    fun cancelTicket(ticket: TripTicket) {
        val index = confirmedTickets.indexOfFirst { it.ticketId == ticket.ticketId }
        if (index != -1) {
            confirmedTickets[index] = confirmedTickets[index].copy(status = TicketStatus.CANCELLED)
        }
    }
}

// =====================================================
// 3. AUTH & SERVICE LAYER
// =====================================================

class AuthManager(context: Context) {
    private val prefs = context.getSharedPreferences("NasugBrowsePrefs", Context.MODE_PRIVATE)
    fun isLoggedIn(): Boolean = prefs.getBoolean("isLoggedIn", false)
    fun getUserName(): String = prefs.getString("userName", "") ?: ""
    fun getUserEmail(): String = prefs.getString("userEmail", "") ?: ""
    fun registerAndLogin(name: String, email: String) {
        prefs.edit().apply { putBoolean("isLoggedIn", true); putString("userName", name); putString("userEmail", email); apply() }
    }
    fun logout() { prefs.edit().apply { putBoolean("isLoggedIn", false); apply() } }
}

object TravelRepository {
    val allDestinations: List<Destination> = listOf(
        Beach(2, "Pico de Loro Beach", "Nasugbu, Batangas", 1500.0, 4.7, R.drawable.pico_de_loro, "A high-end beach club famous for its iconic 'parrot beak' rock formation.", 14.192139, 120.602222, "Partly Cloudy, 32°C", true),
        Beach(5, "Munting Buhangin Beach", "Nasugbu, Batangas", 1500.0, 4.4, R.drawable.munting_buhangin, "A laid-back, family-friendly resort known for its fine grey sand.", 14.113222, 120.622444, "Sunny, 30°C", true),
        Beach(8, "Kayraang Cove", "Brgy. Calayo, Nasugbu", 1200.0, 4.8, R.drawable.kayraang_cove, "A private, protected cove with raw and rugged beauty.", 14.162639, 120.596861, "Sunny, 31°C", false),
        Beach(10, "Santelmo Cove", "Hamilo Coast, Nasugbu", 3000.0, 4.9, R.drawable.santelmo_cove, "A semi-secluded, pristine cove within the Hamilo Coast.", 14.205556, 120.594444, "Partly Cloudy, 30°C", false),
        Beach(11, "Tali Beach", "Brgy. Balaytigue, Nasugbu", 1800.0, 4.6, R.drawable.tali_beach, "A private beach subdivision known for its cliff jump site.", 14.123982, 120.612279, "Breezy, 29°C", true),
        Beach(16, "Natipuan White Sand", "Brgy. Natipuan, Nasugbu", 1800.0, 4.6, R.drawable.natipuan, "A sheltered, crescent-shaped coastal stretch known for fine cream-to-white sand, calm swimming waters, and budget-friendly beachfront resorts.", 14.1174000, 120.6188000, "Sunny, 35°C", true),
        Beach(17, "Pamana Resorts and Hotel", "Brgy. Calayo, Nasugbu", 250.0, 4.7, R.drawable.pamana, "Pairs a laid-back public shoreline—famous for zero entrance/corkage fees—with a pet-friendly beachfront stay offering ₱250 day tours, an infinity pool.", 14.152063, 120.609312, "Sunny, 35°C", true),
        Beach(12, "Canyon Cove by Lawton", "Brgy. Wawa, Nasugbu", 2500.0, 4.8, R.drawable.canyon_cove, "A sheltered crescent cove in Piloto Wawa, Nasugbu, framed by calm turquoise waters, a wide cream-sand beach, and lush green coastal hills.", 14.0933, 120.6197, "Sunny, 32°C", true),
        Beach(18, "Matabungkay Beach Hotel", "Brgy. Matabungkay, Nasugbu", 2500.0, 4.8, R.drawable.matabungkay, "A popular, family-friendly spot best known for its shallow, calm waters, wide cream-sand shoreline, and iconic floating bamboo cottages (balsa).", 13.9475, 120.6169, "Sunny, 32°C", true),
        HistoricalSite(6, "Cape Santiago Lighthouse", "Nasugbu, Batangas", 300.0, 4.6, R.drawable.cape_santiago, "One of the oldest and tallest lighthouses in the Philippines.", 13.77139, 120.65250, "Breezy, 30°C", 50.0),
        HistoricalSite(9, "Caleruega Church", "Brgy. Kaylaway, Nasugbu", 250.0, 4.7, R.drawable.caleruega, "A stunning Romanesque-style chapel perched on a hill.", 14.072583, 120.835806, "Cool, 25°C", 50.0),
        HistoricalSite(14, "Nasugbu Municipal Hall & Plaza", "Poblacion, Nasugbu", 0.0, 4.0, R.drawable.nasugbu_municipal, "The town center, which features a historic municipal hall.", 14.07429, 120.63249, "Humid, 31°C", 0.0),
        AdventureSpot(13, "Pico de Loro (Monte Palay-Palay)", "Nasugbu, Batangas", 700.0, 4.7, R.drawable.pico_de_loro_hike, "A popular hiking destination for its distinctive 'parrot's beak' peak.", 14.2167, 120.6500, "Windy, 23°C", "Hard (6/9)"),
        AdventureSpot(3, "Mt. Batulao", "Nasugbu, Batangas", 800.0, 4.8, R.drawable.mt_batulao, "A popular hiking destination offering breathtaking 360-degree views.", 14.04500, 120.80444, "Cool, 24°C", "Moderate (4/9)"),
        AdventureSpot(4, "Mt. Talamitam", "Nasugbu, Batangas", 600.0, 4.5, R.drawable.mt_talamitam, "A beginner-friendly mountain known for its scenic grasslands.", 14.10778, 120.75972, "Clear, 26°C", "Easy (3/9)"),
        Restaurant(15, "Daddy Riz", "Nasugbu, Batangas", 350.0, 4.4, R.drawable.daddy_riz, "A must-visit for foodies serving the famous Batangas Lomi.", 14.0750, 120.6300, "Warm, 28°C", "Batangas Cuisine"),
        Restaurant(7, "Kainan sa Dalampasigan", "Nasugbu, Batangas", 800.0, 4.5, R.drawable.kainan_sa_dalampasigan, "A famous local dining spot serving authentic Batangas dishes.", 14.072139, 120.625194, "Humid, 31°C", "Batangas Cuisine"),
    )

    val categories = listOf("All", "Beach", "Historical", "Adventure", "Food")

    val availableVehicles = listOf(
        Vehicle(1, "Tour Bus", "Large capacity, air-conditioned", 6000.0, Icons.Default.DirectionsBus, "Kuya Jobert", "09171234567", "BUS-123"),
        Vehicle(2, "Private Van", "Air-conditioned, good for groups", 4500.0, Icons.Default.AirportShuttle, "Mang Tomas", "09187654321", "VAN-456"),
        Vehicle(3, "Private Car", "Sedan or SUV, up to 4 people", 2500.0, Icons.Default.DirectionsCar, "Sir Mark", "09199998888", "CAR-789"),
        Vehicle(4, "Local Jeepney", "Batangas Route, budget friendly", 600.0, Icons.Default.DirectionsBus, "Manong Pedro", "09201112222", "JEEP-000")
    )
}

// =====================================================
// 4. MAIN ACTIVITY & SPLASH
// =====================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = LightColors) {
                val context = LocalContext.current
                val authManager = remember { AuthManager(context) }
                var showSplash by remember { mutableStateOf(true) }
                var isLoggedIn by remember { mutableStateOf(authManager.isLoggedIn()) }

                LaunchedEffect(Unit) { delay(2000); showSplash = false }

                if (showSplash) {
                    SplashScreen()
                } else {
                    if (isLoggedIn) {
                        val booking = remember { BookingSystem(authManager.getUserName(), authManager.getUserEmail()) }
                        MainApp(booking, onLogout = { authManager.logout(); isLoggedIn = false })
                    } else {
                        AuthScreen(authManager) { isLoggedIn = true }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PrimaryBlue, Color(0xFF0F172A)))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.FlightTakeoff, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color.White)
            Spacer(Modifier.height(16.dp))
            Text("NasugBrowse", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = AccentOrange)
        }
    }
}

// =====================================================
// 5. AUTH SCREEN
// =====================================================

@Composable
fun AuthScreen(authManager: AuthManager, onLoginSuccess: () -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BgLight).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(80.dp), tint = PrimaryBlue)
        Spacer(Modifier.height(16.dp))
        Text(if (isLoginMode) "Welcome Back" else "Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text("Browse the best of Nasugbu, Batangas", color = TextGray)
        Spacer(Modifier.height(32.dp))

        if (!isLoginMode) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)); Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)); Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { authManager.registerAndLogin(if (isLoginMode) email.substringBefore("@") else name, email); onLoginSuccess() },
            modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) { Text(if (isLoginMode) "Log In" else "Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { isLoginMode = !isLoginMode }) { Text(if (isLoginMode) "Don't have an account? Sign Up" else "Already have an account? Log In", color = PrimaryBlue) }
    }
}

// =====================================================
// 6. MAIN APP NAVIGATION
// =====================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(booking: BookingSystem, onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedDestinationDetail by remember { mutableStateOf<Destination?>(null) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var selectedTicketDetail by remember { mutableStateOf<TripTicket?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Explore, contentDescription = null) }, label = { Text("Explore") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Favorite, contentDescription = null) }, label = { Text("Saved") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
                NavigationBarItem(
                    selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    icon = { BadgedBox(badge = { if (booking.cartDestinations.isNotEmpty()) Badge { Text(booking.cartDestinations.size.toString()) } }) { Icon(Icons.Default.ShoppingCart, contentDescription = null) } },
                    label = { Text("Cart") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue)
                )
                NavigationBarItem(
                    selected = selectedTab == 3, onClick = { selectedTab = 3 },
                    icon = { BadgedBox(badge = { if (booking.confirmedTickets.isNotEmpty()) Badge { Text(booking.confirmedTickets.size.toString()) } }) { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) } },
                    label = { Text("Tickets") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue)
                )
                NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 }, icon = { Icon(Icons.Default.Person, contentDescription = null) }, label = { Text("Profile") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).background(BgLight)) {
            when (selectedTab) {
                0 -> ExploreScreen(booking, onDetails = { selectedDestinationDetail = it }, snackbar = { scope.launch { snackbarHostState.showSnackbar(it) } })
                1 -> FavoritesScreen(booking, onDetails = { selectedDestinationDetail = it })
                2 -> CartScreen(booking, onCheckout = { showCheckoutSheet = true }, snackbar = { scope.launch { snackbarHostState.showSnackbar(it) } })
                3 -> TicketsScreen(booking, onViewTicket = { selectedTicketDetail = it })
                4 -> ProfileScreen(booking, onLogout)
            }
        }

        if (selectedDestinationDetail != null) {
            DestinationDetailSheet(selectedDestinationDetail!!, onDismiss = { selectedDestinationDetail = null })
        }

        if (showCheckoutSheet) {
            CheckoutSheet(booking, onDismiss = { showCheckoutSheet = false }, onSuccess = { paymentMethod ->
                showCheckoutSheet = false
                booking.confirmBooking(paymentMethod)
                scope.launch { snackbarHostState.showSnackbar("Booking Confirmed! Ticket Generated.") }
                selectedTab = 3
            })
        }

        if (selectedTicketDetail != null) {
            TicketDetailSheet(
                ticket = selectedTicketDetail!!,
                onDismiss = { selectedTicketDetail = null },
                onCancel = { booking.cancelTicket(selectedTicketDetail!!); selectedTicketDetail = null }
            )
        }
    }
}

// =====================================================
// 7. SCREENS
// =====================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(booking: BookingSystem, onDetails: (Destination) -> Unit, snackbar: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val filtered = TravelRepository.allDestinations.filter { dest ->
        val matchesCategory = selectedCategory == "All" || dest.category() == selectedCategory
        val matchesSearch = dest.name.contains(searchQuery, ignoreCase = true) || dest.location.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Hi, ${booking.ownerName.split(" ").first()} 👋", fontSize = 16.sp, color = TextGray, modifier = Modifier.padding(top = 16.dp))
        Text("Browse Destinations", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextDark, modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search destinations...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TravelRepository.categories) { category ->
                FilterChip(
                    selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue, selectedLabelColor = Color.White)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(filtered, key = { it.id }) { dest ->
                val isAdded = booking.cartDestinations.any { it.id == dest.id }
                val isFav = booking.favoriteDestinations.any { it.id == dest.id }
                ProfessionalDestinationCard(dest, isAdded, isFav, onClick = { onDetails(dest) }, onFavToggle = { booking.toggleFavorite(dest) }) {
                    booking.toggleCart(dest)
                    if (!isAdded) snackbar("Added to Cart") else snackbar("Removed from Cart")
                }
            }
        }
    }
}

@Composable
fun ProfessionalDestinationCard(dest: Destination, isAdded: Boolean, isFav: Boolean, onClick: () -> Unit, onFavToggle: () -> Unit, onAddToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(220.dp).clickable { onClick() }, shape = RoundedCornerShape(20.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = dest.imageResId),
                contentDescription = dest.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 300f)))
            IconButton(onClick = onFavToggle, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).background(Color.White.copy(alpha = 0.3f), CircleShape)) {
                Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isFav) Color.Red else Color.White)
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(dest.name, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                    Text(dest.location, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("₱${"%.0f".format(dest.baseCost)}", fontWeight = FontWeight.ExtraBold, color = AccentOrange, fontSize = 20.sp)
                    FilledIconButton(onClick = onAddToggle, colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryBlue)) {
                        Icon(if (isAdded) Icons.Default.Check else Icons.Default.Add, contentDescription = null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(booking: BookingSystem, onCheckout: () -> Unit, snackbar: (String) -> Unit) {
    val context = LocalContext.current
    val dateFormatter = DateFormat.getMediumDateFormat(context)
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    val cal = Calendar.getInstance().apply { timeInMillis = it }
                    booking.startDate = cal.time; cal.add(Calendar.DAY_OF_YEAR, 3); booking.endDate = cal.time
                }
                showDatePicker = false
            }) { Text("Confirm") }
        }) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val amPm = if (hourOfDay < 12) "AM" else "PM"
                val displayHour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                booking.startTime = String.format("%02d:%02d %s", displayHour, minute, amPm)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    val isVehicleSelected = booking.selectedVehicle != null
    val isTimeSelected = booking.startTime != null
    val isFormComplete = isVehicleSelected && isTimeSelected

    Column(modifier = Modifier.fillMaxSize()) {
        Text("My Booking Cart", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextDark, modifier = Modifier.padding(16.dp))

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryBlue)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Travel Dates", fontWeight = FontWeight.Bold, color = TextDark)
                                    Text(if (booking.startDate != null) "${dateFormatter.format(booking.startDate)} - ${dateFormatter.format(booking.endDate)}" else "Select dates", fontSize = 12.sp, color = TextGray)
                                }
                            }
                            TextButton(onClick = { showDatePicker = true }) { Text("Pick", color = PrimaryBlue) }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BgLight)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = PrimaryBlue)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Start Time", fontWeight = FontWeight.Bold, color = TextDark)
                                    Text(booking.startTime ?: "Select time", fontSize = 12.sp, color = TextGray)
                                }
                            }
                            TextButton(onClick = { showTimePicker = true }) { Text("Pick", color = PrimaryBlue) }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BgLight)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.People, contentDescription = null, tint = PrimaryBlue)
                                Spacer(Modifier.width(8.dp))
                                Text("Travelers", fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (booking.travelers > 1) booking.travelers-- }) { Icon(Icons.Default.RemoveCircleOutline, null, tint = TextGray) }
                                Text("${booking.travelers}", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { booking.travelers++ }) { Icon(Icons.Default.AddCircleOutline, null, tint = PrimaryBlue) }
                            }
                        }
                    }
                }
            }
            item { Text("Destinations", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark) }
            if (booking.cartDestinations.isEmpty()) {
                item { Text("Your cart is empty.", color = TextGray) }
            } else {
                items(booking.cartDestinations, key = { it.id }) { dest ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(dest.icon(), contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dest.name, fontWeight = FontWeight.Bold, color = TextDark)
                                Text("₱${"%.0f".format(dest.baseCost)} x ${booking.travelers} pax", fontSize = 12.sp, color = TextGray)
                            }
                            Text("₱${"%.0f".format(dest.costForTravelers(booking.travelers))}", fontWeight = FontWeight.Bold, color = TextDark)
                        }
                    }
                }
            }

            item {
                Text("Transportation (Required)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                if (!isFormComplete) {
                    Text("⚠️ Please select travel dates, time, and transportation to proceed.", fontSize = 12.sp, color = Color.Red)
                }
            }
            items(TravelRepository.availableVehicles) { vehicle ->
                val isSelected = booking.selectedVehicle?.id == vehicle.id
                Card(
                    modifier = Modifier.clickable { booking.selectedVehicle = vehicle },
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.White),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlue) else null
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(vehicle.icon, contentDescription = null, tint = if (isSelected) PrimaryBlue else TextGray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(vehicle.name, fontWeight = FontWeight.Bold, color = TextDark)
                            Text(vehicle.type, fontSize = 12.sp, color = TextGray)
                        }
                        Text("₱${"%.0f".format(vehicle.baseFare)}", fontWeight = FontWeight.Bold, color = TextDark)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        if (booking.cartDestinations.isNotEmpty()) {
            Surface(shadowElevation = 16.dp, color = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Total Estimate", color = TextGray, fontSize = 12.sp)
                            Text("₱${"%.2f".format(booking.calculateTotal())}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = PrimaryBlue)
                        }
                        Button(
                            onClick = {
                                if (isFormComplete) onCheckout()
                                else snackbar("Please complete the form first.")
                            },
                            enabled = isFormComplete,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                disabledContainerColor = TextGray.copy(alpha = 0.3f)
                            )
                        ) {
                            Text("Proceed to Pay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketsScreen(booking: BookingSystem, onViewTicket: (TripTicket) -> Unit) {
    if (booking.confirmedTickets.isEmpty()) {
        EmptyState("No tickets yet", "Your booked trips will appear here.")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("My E-Tickets", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextDark, modifier = Modifier.padding(bottom = 8.dp)) }
            items(booking.confirmedTickets) { ticket ->
                TicketCard(ticket) { onViewTicket(ticket) }
            }
        }
    }
}

@Composable
fun TicketCard(ticket: TripTicket, onClick: () -> Unit) {
    val isCancelled = ticket.status == TicketStatus.CANCELLED
    val bgColor = if (isCancelled) Color(0xFFFEE2E2) else Color.White
    val textColor = if (isCancelled) Color(0xFF991B1B) else TextDark

    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bgColor), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = if (isCancelled) textColor else PrimaryBlue)
                    Spacer(Modifier.width(8.dp))
                    Text("NASUGBROWSE", fontWeight = FontWeight.ExtraBold, color = textColor, fontSize = 14.sp)
                }
                Text(if (isCancelled) "CANCELLED" else "CONFIRMED", fontWeight = FontWeight.Bold, color = if(isCancelled) textColor else Color(0xFF16A34A), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("${ticket.destinations.size} Destinations • ${ticket.travelers} Travelers", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text("Total Paid: ₱${"%.2f".format(ticket.totalPaid)}", color = TextGray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(30) { Box(modifier = Modifier.width(6.dp).height(2.dp).background(TextGray).padding(horizontal = 1.dp)) }
            }

            Spacer(Modifier.height(8.dp))
            Text("Tap to view Receipt", fontSize = 12.sp, color = PrimaryBlue, textDecoration = TextDecoration.Underline)
        }
    }
}

// =====================================================
// 8. BOTTOM SHEETS (MODALS)
// =====================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationDetailSheet(dest: Destination, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Image(
                painter = painterResource(id = dest.imageResId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(dest.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                Text(dest.location, color = TextGray, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(18.dp)); Text(" ${dest.rating} • ${dest.category()}", fontWeight = FontWeight.Bold, color = TextDark) }
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.WbSunny, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(18.dp)); Text(" ${dest.weather}", fontSize = 14.sp, color = TextDark) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(dest.description, fontSize = 15.sp, color = TextDark, lineHeight = 24.sp)
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val gmmIntentUri = Uri.parse("google.navigation:q=${dest.latitude},${dest.longitude}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply { setPackage("com.google.android.apps.maps") }
                    if (mapIntent.resolveActivity(context.packageManager) != null) context.startActivity(mapIntent)
                    else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${dest.latitude},${dest.longitude}")))
                },
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) { Icon(Icons.Default.Directions, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Navigate with Google Maps", fontSize = 16.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSheet(booking: BookingSystem, onDismiss: () -> Unit, onSuccess: (String) -> Unit) {
    val paymentOptions = listOf("Credit/Debit Card", "GCash / Online", "Pay in Cash")
    var selectedMethod by remember { mutableStateOf("Credit/Debit Card") }

    var cardNumber by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }

    var isProcessing by remember { mutableStateOf(false) }
    var isPaymentComplete by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {

            if (isPaymentComplete) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF16A34A))
                    Spacer(Modifier.height(16.dp))
                    Text("Payment Complete!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Your trip has been successfully booked.", color = TextGray)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onSuccess(selectedMethod) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("View My Ticket", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text("Payment Method", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                Spacer(Modifier.height(16.dp))

                paymentOptions.forEach { method ->
                    val isSelected = selectedMethod == method
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedMethod = method }
                            .background(if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = { selectedMethod = method }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue))
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = when(method) { "Credit/Debit Card" -> Icons.Default.CreditCard; "GCash / Online" -> Icons.Default.QrCode; else -> Icons.Default.Money },
                            contentDescription = null, tint = PrimaryBlue
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(method, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = TextDark)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = BgLight)

                when (selectedMethod) {
                    "Credit/Debit Card" -> {
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { if (it.length <= 16) cardNumber = it },
                            label = { Text("Card Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    "GCash / Online" -> {
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            label = { Text("GCash/Maya Mobile Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp).border(1.dp, TextGray, RoundedCornerShape(12.dp)).background(BgLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.QrCode2, contentDescription = "QR Code", modifier = Modifier.size(100.dp), tint = PrimaryBlue)
                                Text("Scan to Pay with GCash", fontSize = 12.sp, color = TextGray)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    "Pay in Cash" -> {
                        Text("Please prepare the exact amount upon boarding. A confirmation email will be sent to you.", fontSize = 14.sp, color = TextGray)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total to Pay", fontWeight = FontWeight.Bold, color = TextGray)
                    Text("₱${"%.2f".format(booking.calculateTotal())}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = PrimaryBlue)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedMethod == "Credit/Debit Card" && cardNumber.length < 16) {
                        } else {
                            isProcessing = true
                            kotlinx.coroutines.MainScope().launch {
                                delay(1500)
                                isProcessing = false
                                isPaymentComplete = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Pay ₱${"%.2f".format(booking.calculateTotal())}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailSheet(ticket: TripTicket, onDismiss: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val dateFormatter = DateFormat.getMediumDateFormat(context)
    val isCancelled = ticket.status == TicketStatus.CANCELLED

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Receipt", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                if (isCancelled) {
                    Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(8.dp)) { Text("CANCELLED", color = Color(0xFF991B1B), modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Ticket ID: ${ticket.ticketId}", fontWeight = FontWeight.Bold, color = TextDark)
            Text("Date Booked: ${dateFormatter.format(ticket.bookingDate)}", fontSize = 14.sp, color = TextGray)
            ticket.startTime?.let {
                Text("Start Time: $it", fontSize = 14.sp, color = TextGray)
            }
            Text("Payment: ${ticket.paymentMethod}", fontSize = 14.sp, color = TextGray)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = BgLight)

            Text("Destinations:", fontWeight = FontWeight.Bold, color = TextDark)
            ticket.destinations.forEach { Text("- ${it.name} (x${ticket.travelers})", fontSize = 14.sp, color = TextGray) }

            Spacer(modifier = Modifier.height(12.dp))

            if (ticket.vehicle != null) {
                Text("Driver Details:", fontWeight = FontWeight.Bold, color = TextDark)
                Text("Name: ${ticket.vehicle.driverName}", fontSize = 14.sp, color = TextGray)
                Text("Plate No: ${ticket.vehicle.plateNumber}", fontSize = 14.sp, color = TextGray)
                Text("Phone: ${ticket.vehicle.driverPhone}", fontSize = 14.sp, color = TextGray)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ticket.vehicle.driverPhone}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Driver", fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Transportation:", fontWeight = FontWeight.Bold, color = TextDark)
                Text("- None", fontSize = 14.sp, color = TextGray)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = BgLight)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL PAID", fontWeight = FontWeight.Bold, color = TextDark)
                Text("₱${"%.2f".format(ticket.totalPaid)}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = PrimaryBlue)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth().height(60.dp).background(TextDark, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text("|||| ||| || |||| ||| || |||| ||| ||", color = Color.White, letterSpacing = 4.sp, fontSize = 20.sp, modifier = Modifier.alpha(0.8f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isCancelled) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Cancel Booking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =====================================================
// 9. REUSABLE & PROFILE
// =====================================================

@Composable
fun FavoritesScreen(booking: BookingSystem, onDetails: (Destination) -> Unit) {
    if (booking.favoriteDestinations.isEmpty()) {
        EmptyState("No saved places", "Tap the heart icon to save it here.")
    } else {
        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(booking.favoriteDestinations, key = { it.id }) { dest ->
                Card(modifier = Modifier.clickable { onDetails(dest) }, shape = RoundedCornerShape(16.dp)) {
                    Column {
                        Image(
                            painter = painterResource(id = dest.imageResId),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.height(120.dp).fillMaxWidth()
                        )
                        Text(dest.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(booking: BookingSystem, onLogout: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(32.dp))
        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(100.dp), tint = PrimaryBlue)
        Text(booking.ownerName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text(booking.ownerEmail, color = TextGray)
        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Saved Destinations", color = TextDark); Text("${booking.favoriteDestinations.size}", fontWeight = FontWeight.Bold, color = PrimaryBlue) }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BgLight)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Active Tickets", color = TextDark); Text("${booking.confirmedTickets.count { it.status == TicketStatus.ACTIVE }}", fontWeight = FontWeight.Bold, color = PrimaryBlue) }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626))
        ) {
            Icon(Icons.Default.Logout, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Log Out", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(80.dp), tint = TextGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
        Text(subtitle, textAlign = TextAlign.Center, color = TextGray)
    }
}
package com.origin.browser.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origin.browser.R
import com.origin.browser.ui.theme.*
import java.util.Calendar
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    isAdBlockEnabled: Boolean,
    isStealthMode: Boolean,
    tabsCount: Int = 0,
    onSearch: (String) -> Unit,
    onChangeName: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleStealthMode: () -> Unit,
    onToggleAdBlock: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val calendar = remember { Calendar.getInstance() }
    val hour = remember { calendar.get(Calendar.HOUR_OF_DAY) }
    val dayOfWeek = remember { calendar.get(Calendar.DAY_OF_WEEK) }
    val seed = remember { Random.nextInt() }
    val greetingTitle = remember(hour, dayOfWeek, seed) {
        fun pick(vararg options: String) = options[seed % options.size]

        val morning = arrayOf("Good Morning", "Morning", "Rise and Shine", "Morning Sunshine", "Hello There")
        val afternoon = arrayOf("Good Afternoon", "Happy Afternoon", "Afternoon Vibes", "Stay Productive", "Hope Your Day Is Going Well")
        val evening = arrayOf("Good Evening", "Evening", "Hope You Had a Good Day", "Wind Down Time", "Evening Chill")
        val night = arrayOf("Hey Night Owl", "Late Night Browsing", "Still Up?", "Moonlight Browsing", "Midnight Mode")
        val weekend = arrayOf("Happy Weekend", "Weekend Vibes", "Enjoy Your Weekend", "Weekend Mode", "Time to Relax")
        val sunday = arrayOf("Happy Sunday", "Sunday Vibes", "Relax and Recharge", "Slow Sunday", "Easy Like Sunday Morning")
        val monday = arrayOf("Happy Monday", "New Week New Goals", "Monday Motivation", "Rise and Grind", "Fresh Start")
        val friday = arrayOf("Happy Friday", "Friday Feels", "Weekend Loading...", "TGIF", "Friday Vibes")

        val slot = when (hour) {
            in 0..3 -> 0
            in 4..5 -> 1
            in 6..7 -> 2
            in 8..9 -> 3
            in 10..11 -> 4
            in 12..13 -> 5
            in 14..15 -> 6
            in 16..17 -> 7
            in 18..19 -> 8
            in 20..21 -> 9
            else -> 10
        }

        when (slot) {
            0 -> when (dayOfWeek) {
                Calendar.SATURDAY, Calendar.SUNDAY -> pick("Late Night Weekend", "Weekend Night Owl", "After Hours")
                else -> pick(*night)
            }
            1 -> when (dayOfWeek) {
                Calendar.SATURDAY, Calendar.SUNDAY -> pick("Weekend Early Bird", "Early Riser", "Up Before the Sun")
                Calendar.FRIDAY -> pick("Early Start", "TGIF!", "Weekend Bound")
                else -> pick("Rise and Shine", "Early Bird", "Daybreak", "Up Before the Sun")
            }
            2 -> when (dayOfWeek) {
                Calendar.SATURDAY, Calendar.SUNDAY -> pick("Good Morning", "Happy Weekend", "Peaceful Morning", "Weekend Sunshine")
                Calendar.FRIDAY -> pick("TGIF!", "Friday Morning", "Good Morning")
                else -> pick("Good Morning", "Morning Sunshine", "Sunrise Mode", "Fresh Start", "Another Beautiful Day")
            }
            3 -> when (dayOfWeek) {
                Calendar.SATURDAY, Calendar.SUNDAY -> pick(*weekend)
                Calendar.MONDAY -> pick(*monday)
                Calendar.FRIDAY -> pick(*friday)
                Calendar.WEDNESDAY -> pick("Happy Hump Day", "Midweek Momentum", "Over the Hump")
                else -> pick(*morning)
            }
            4 -> when (dayOfWeek) {
                Calendar.SATURDAY, Calendar.SUNDAY -> pick("Weekend Bliss", "No Plans? Perfect", "Sip Back and Relax")
                Calendar.MONDAY -> pick("Go Get 'Em", "Week Ahead", "Monday Hustle", "Crush This Week")
                Calendar.FRIDAY -> pick("Friday Energy", "Weekend Eve", "Let the Weekend Begin")
                Calendar.WEDNESDAY -> pick("Midweek Hustle", "Keep the Pace", "Wednesday Momentum")
                else -> pick("Stay Productive", "Keep Going", "You're on a Roll")
            }
            5 -> when (dayOfWeek) {
                Calendar.SATURDAY, Calendar.SUNDAY -> pick("Weekend Lunch", "Enjoy Your Day", "Weekend Chill", "Good Times")
                Calendar.FRIDAY -> pick("Happy Friday", "Lunch and Celebrate", "Weekend Starts Now")
                Calendar.SUNDAY -> pick(*sunday)
                else -> pick("Happy Lunch Hour", "Fuel Up", "Midday Break", "Lunch Time", "Recharge")
            }
            6 -> when (dayOfWeek) {
                Calendar.SATURDAY, Calendar.SUNDAY -> pick("Weekend Vibes", "Afternoon Relaxation", "Weekend Reset")
                Calendar.FRIDAY -> pick("Friday Frenzy", "Weekend's Calling", "Freedom Friday", "Last Push")
                Calendar.THURSDAY -> pick("Almost Friday", "The End Is Near", "Final Stretch")
                else -> pick(*afternoon)
            }
            7 -> when (dayOfWeek) {
                Calendar.SATURDAY, Calendar.SUNDAY -> pick("Enjoy Your Weekend", "Easy Afternoon", "Weekend Browsing")
                Calendar.FRIDAY -> pick("Weekend Is Here", "Let the Weekend Begin", "Celebrate Good Times")
                Calendar.THURSDAY -> pick("Weekend Loading...", "Almost Weekend")
                else -> pick(*afternoon)
            }
            8 -> when (dayOfWeek) {
                Calendar.SATURDAY, Calendar.SUNDAY -> pick("Good Evening", "Weekend Nights", "Evening Enjoyment")
                Calendar.FRIDAY -> pick("Weekend Mode", "Happy Friday Night", "Weekend Begins")
                else -> pick(*evening)
            }
            9 -> when (dayOfWeek) {
                Calendar.SATURDAY -> pick("Weekend Vibes", "Saturday Night", "Evening Chill")
                Calendar.SUNDAY -> pick("Hope You Had a Nice Weekend", "Sunday Night Feels", "Ready for the Week?")
                Calendar.FRIDAY -> pick("Happy Friday Night", "Friday Night Feels", "Weekend Mode")
                Calendar.MONDAY -> pick("Hope You Had a Great Day", "Good Night", "Monday Done")
                else -> pick("Hope You Had a Good Day", "Evening Relaxation", "How Was Your Day")
            }
            else -> when (dayOfWeek) {
                Calendar.SATURDAY -> pick("Weekend Nights", "Late Weekend Browsing")
                Calendar.SUNDAY -> pick("Sunday Night Feels", "Late Sunday", "Calm Before the Week")
                Calendar.FRIDAY -> pick("Friday Night Feels", "Weekend Begins", "Late Night Friday")
                else -> pick("Getting Late", "Time to Rest", "Good Night", "Wind Down")
            }
        }
    }

    val displayName = userName.ifBlank { "Explorer" }
    val ntype82 = FontFamily(Font(R.font.ntype82_regular))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OriginDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top-left icons row
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp)
        ) {
            IconButton(
                onClick = onOpenBookmarks,
                modifier = Modifier.size(40.dp)
            ) {
                Text(
                    text = "★",
                    color = OriginWhite,
                    fontSize = 20.sp
                )
            }
            IconButton(
                onClick = onOpenTabs,
                modifier = Modifier.size(40.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_tabs),
                    contentDescription = "Tabs",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        // Settings icon (top right)
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
                .size(40.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Settings",
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            // App UI Logo (clickable to open menu)
            var isMenuOpen by remember { mutableStateOf(false) }
            Box {
                Image(
                    painter = painterResource(id = if (isStealthMode) R.drawable.ic_stealth_mode else R.drawable.ic_app_logo),
                    contentDescription = "Origin Browser Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 24.dp)
                        .clickable { isMenuOpen = true }
                )
                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false },
                    modifier = Modifier
                        .background(OriginDarkBackground.copy(alpha = 0.75f))
                        .border(1.dp, OriginOutline, RoundedCornerShape(8.dp))
                ) {
                    Surface(
                        color = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        Column {
                            DropdownMenuItem(
                                text = { Text("Bookmarks", color = OriginWhite) },
                                leadingIcon = { Image(painter = painterResource(id = R.drawable.ic_bookmark), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    isMenuOpen = false
                                    onOpenBookmarks()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isAdBlockEnabled) "Ad Blocker: ON" else "Ad Blocker: OFF",
                                        color = if (isAdBlockEnabled) Color(0xFF6BFFB8) else OriginWhite
                                    )
                                },
                                leadingIcon = { Image(painter = painterResource(id = R.drawable.ic_ad_blocker), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    isMenuOpen = false
                                    onToggleAdBlock()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isStealthMode) "Stealth Mode: ON" else "Stealth Mode: OFF",
                                        color = if (isStealthMode) Color(0xFF6BFFB8) else OriginWhite
                                    )
                                },
                                leadingIcon = { Image(painter = painterResource(id = R.drawable.ic_stealth_mode), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    isMenuOpen = false
                                    onToggleStealthMode()
                                }
                            )
                    }
                }
            }
            }

            // Dynamic welcome heading in serif font (Clickable to edit name)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onChangeName() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$greetingTitle, $displayName",
                        fontFamily = ntype82,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = OriginWhite
                    )
                }
            }

            // Horizontal input group: Pill search field + Circular search button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Pill-shaped text field
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            text = "Type here or search a URL",
                            color = OriginMutedText,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OriginWhite,
                        unfocusedBorderColor = OriginWhite,
                        focusedTextColor = OriginWhite,
                        unfocusedTextColor = OriginWhite,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = OriginWhite
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (textInput.isNotBlank()) {
                            onSearch(textInput.trim())
                        }
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Circular search button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(1.dp, OriginWhite, CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onSearch(textInput.trim())
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
package org.semanticweb.hermit.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta de colores profesional para herramientas técnicas
object HermitColors {
    // Colores primarios - Azul profundo profesional
    val Primary = Color(0xFF1565C0)           // Azul profundo
    val OnPrimary = Color(0xFFFFFFFF)         // Blanco
    val PrimaryContainer = Color(0xFFE3F2FD)  // Azul muy claro
    val OnPrimaryContainer = Color(0xFF0D47A1) // Azul muy oscuro
    
    // Colores secundarios - Verde técnico
    val Secondary = Color(0xFF2E7D32)         // Verde profesional
    val OnSecondary = Color(0xFFFFFFFF)       // Blanco
    val SecondaryContainer = Color(0xFFE8F5E8) // Verde muy claro
    val OnSecondaryContainer = Color(0xFF1B5E20) // Verde muy oscuro
    
    // Colores terciarios - Naranja técnico
    val Tertiary = Color(0xFFE65100)          // Naranja profesional
    val OnTertiary = Color(0xFFFFFFFF)        // Blanco
    val TertiaryContainer = Color(0xFFFFF3E0) // Naranja muy claro
    val OnTertiaryContainer = Color(0xFFBF360C) // Naranja muy oscuro
    
    // Colores de fondo
    val Background = Color(0xFFFAFAFA)        // Gris muy claro
    val OnBackground = Color(0xFF1A1A1A)      // Casi negro
    val Surface = Color(0xFFFFFFFF)           // Blanco puro
    val OnSurface = Color(0xFF1A1A1A)         // Casi negro
    val SurfaceVariant = Color(0xFFF5F5F5)    // Gris claro
    val OnSurfaceVariant = Color(0xFF424242)  // Gris medio
    
    // Colores de estado
    val Error = Color(0xFFD32F2F)             // Rojo error
    val OnError = Color(0xFFFFFFFF)           // Blanco
    val ErrorContainer = Color(0xFFFFEBEE)    // Rojo muy claro
    val OnErrorContainer = Color(0xFFB71C1C)  // Rojo muy oscuro
    
    // Colores específicos para ontologías
    val Success = Color(0xFF388E3C)           // Verde éxito
    val Warning = Color(0xFFF57C00)           // Naranja advertencia
    val Info = Color(0xFF1976D2)              // Azul información
    
    // Colores para el grafo
    val GraphNode = Color(0xFF3F51B5)         // Índigo para nodos
    val GraphEdge = Color(0xFF757575)         // Gris para enlaces
    val GraphSelected = Color(0xFFFF5722)     // Naranja para selección
    val GraphBackground = Color(0xFFFAFAFA)   // Fondo del grafo
}

// Esquema de colores claro
private val LightColorScheme = lightColorScheme(
    primary = HermitColors.Primary,
    onPrimary = HermitColors.OnPrimary,
    primaryContainer = HermitColors.PrimaryContainer,
    onPrimaryContainer = HermitColors.OnPrimaryContainer,
    
    secondary = HermitColors.Secondary,
    onSecondary = HermitColors.OnSecondary,
    secondaryContainer = HermitColors.SecondaryContainer,
    onSecondaryContainer = HermitColors.OnSecondaryContainer,
    
    tertiary = HermitColors.Tertiary,
    onTertiary = HermitColors.OnTertiary,
    tertiaryContainer = HermitColors.TertiaryContainer,
    onTertiaryContainer = HermitColors.OnTertiaryContainer,
    
    background = HermitColors.Background,
    onBackground = HermitColors.OnBackground,
    surface = HermitColors.Surface,
    onSurface = HermitColors.OnSurface,
    surfaceVariant = HermitColors.SurfaceVariant,
    onSurfaceVariant = HermitColors.OnSurfaceVariant,
    
    error = HermitColors.Error,
    onError = HermitColors.OnError,
    errorContainer = HermitColors.ErrorContainer,
    onErrorContainer = HermitColors.OnErrorContainer
)

@Composable
fun HermitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = HermitTypography,
        content = content
    )
}

// Tipografía personalizada
val HermitTypography = Typography(
    // Títulos principales
    headlineLarge = Typography().headlineLarge,
    headlineMedium = Typography().headlineMedium,
    headlineSmall = Typography().headlineSmall,
    
    // Títulos de sección
    titleLarge = Typography().titleLarge.copy(
        color = HermitColors.Primary
    ),
    titleMedium = Typography().titleMedium.copy(
        color = HermitColors.Primary
    ),
    titleSmall = Typography().titleSmall.copy(
        color = HermitColors.Primary
    ),
    
    // Texto del cuerpo - Usar colores por defecto para respetar el contexto (botones, etc.)
    bodyLarge = Typography().bodyLarge,
    bodyMedium = Typography().bodyMedium,
    bodySmall = Typography().bodySmall,
    
    // Etiquetas
    labelLarge = Typography().labelLarge,
    labelMedium = Typography().labelMedium,
    labelSmall = Typography().labelSmall
)


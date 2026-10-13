package org.semanticweb.hermit.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.semanticweb.hermit.ui.SubClassRelation
import org.semanticweb.hermit.ui.theme.HermitColors
import kotlin.math.*
import kotlin.random.Random

@Composable
fun OntologyGraphView(
    classes: List<String>,
    objectProperties: List<String>,
    dataProperties: List<String>,
    individuals: List<String>,
    subClassRelations: List<SubClassRelation>
) {
    var selectedNode by remember { mutableStateOf<GraphNode?>(null) }
    var showLegend by remember { mutableStateOf(true) }
    
    if (classes.isEmpty() && objectProperties.isEmpty() && dataProperties.isEmpty() && individuals.isEmpty()) {
        EmptyGraphState()
        return
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Grafo de Ontología",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showLegend = !showLegend }
                ) {
                    Text(if (showLegend) "Ocultar Leyenda" else "Mostrar Leyenda", 
                         style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main graph area
            Card(
                modifier = Modifier.weight(if (showLegend) 0.7f else 1f)
            ) {
                InteractiveOntologyGraph(
                    classes = classes,
                    objectProperties = objectProperties,
                    dataProperties = dataProperties,
                    individuals = individuals,
                    subClassRelations = subClassRelations,
                    onNodeSelected = { selectedNode = it }
                )
            }
            
            // Legend and details
            if (showLegend) {
                Card(
                    modifier = Modifier.weight(0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GraphLegend()
                        
                        selectedNode?.let { node ->
                            Divider()
                            NodeDetails(node)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveOntologyGraph(
    classes: List<String>,
    objectProperties: List<String>,
    dataProperties: List<String>,
    individuals: List<String>,
    subClassRelations: List<SubClassRelation>,
    onNodeSelected: (GraphNode) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val nodeLabelStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color.Black)
    
    // Generate graph data
    val graphData = remember(classes, objectProperties, dataProperties, individuals, subClassRelations) {
        generateGraphData(classes, objectProperties, dataProperties, individuals, subClassRelations)
    }
    
    var canvasSize by remember { mutableStateOf(Offset.Zero) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas for drawing nodes and edges
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        // Handle node clicking
                        val clickPos = change.position
                        val clickedNode = graphData.nodes.find { node ->
                            val distance = sqrt(
                                (clickPos.x - node.position.x).pow(2) + 
                                (clickPos.y - node.position.y).pow(2)
                            )
                            distance <= 30f
                        }
                        clickedNode?.let { onNodeSelected(it) }
                    }
                }
        ) {
            canvasSize = Offset(size.width, size.height)
            
            // Update node positions if canvas size changed
            if (graphData.nodes.any { it.position == Offset.Zero }) {
                updateNodePositions(graphData.nodes, size.width, size.height)
            }
            
            // Draw clusters behind everything
            graphData.clusters.forEach { cluster ->
                drawCluster(cluster, graphData.nodes)
            }
            
            // Draw edges (behind nodes)
            graphData.edges.forEach { edge ->
                val fromNode = graphData.nodes.find { it.id == edge.from }
                val toNode = graphData.nodes.find { it.id == edge.to }
                if (fromNode != null && toNode != null) {
                    drawEdge(fromNode.position, toNode.position, edge.color, edge.dashed)
                }
            }
            
            // Draw nodes and their labels
            graphData.nodes.forEach { node ->
                drawNode(node)
                drawNodeLabel(node, textMeasurer, nodeLabelStyle)
            }
        }
        
        // (edge labels remain overlayed for readability)
        
        // Overlay labels for clusters
        graphData.clusters.forEach { cluster ->
            val bounds = computeClusterBounds(cluster, graphData.nodes)
            ClusterLabel(
                text = cluster.label,
                topLeft = bounds.first,
                size = bounds.second
            )
        }
        
        // Overlay text labels for edges
        graphData.edges.forEach { edge ->
            val fromNode = graphData.nodes.find { it.id == edge.from }
            val toNode = graphData.nodes.find { it.id == edge.to }
            if (fromNode != null && toNode != null && fromNode.position != Offset.Zero && toNode.position != Offset.Zero) {
                val midPoint = Offset(
                    (fromNode.position.x + toNode.position.x) / 2,
                    (fromNode.position.y + toNode.position.y) / 2
                )
                EdgeLabel(
                    text = when (edge.type) {
                        EdgeType.SUBCLASS -> "subClassOf"
                        EdgeType.PROPERTY_DOMAIN -> "domain"
                        EdgeType.PROPERTY_RANGE -> "range"
                        EdgeType.INSTANCE_OF -> "instanceOf"
                    },
                    position = midPoint
                )
            }
        }
    }
}

@Composable
fun NodeLabel(
    text: String,
    position: Offset
) {
    // Better positioned labels with proper scaling
    Box(
        modifier = Modifier
            .offset(
                x = (position.x * 0.8f).dp, // Better scaling factor
                y = (position.y * 0.8f + 35).dp // Position below nodes
            )
            .background(
                Color.White.copy(alpha = 0.9f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            color = Color.Black
        )
    }
}

@Composable
fun EdgeLabel(
    text: String,
    position: Offset
) {
    Box(
        modifier = Modifier
            .offset(
                x = (position.x * 0.8f).dp,
                y = (position.y * 0.8f).dp
            )
            .background(
                HermitColors.Warning.copy(alpha = 0.9f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = Color.Black
        )
    }
}

@Composable
fun GraphLegend() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Leyenda",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        LegendItem("Clases", HermitColors.Primary, NodeType.CLASS)
        LegendItem("Prop. Objeto", HermitColors.Secondary, NodeType.OBJECT_PROPERTY)
        LegendItem("Prop. Datos", HermitColors.Tertiary, NodeType.DATA_PROPERTY)
        LegendItem("Individuos", HermitColors.Error, NodeType.INDIVIDUAL)
        LegendItem("Metamodelado", Color(0xFF9C27B0), NodeType.METAMODEL)
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Canvas(modifier = Modifier.size(20.dp, 2.dp)) {
                drawLine(
                    color = Color.Gray,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx()
                )
            }
            Text("SubClase", style = MaterialTheme.typography.bodySmall)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Canvas(modifier = Modifier.size(20.dp, 2.dp)) {
                drawLine(
                    color = Color.Red,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }
            Text("Metamodelo", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, nodeType: NodeType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(if (nodeType == NodeType.INDIVIDUAL) RoundedCornerShape(4.dp) else CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun NodeDetails(node: GraphNode) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Seleccionado",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = node.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "Tipo: ${node.type.displayName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (node.connections.isNotEmpty()) {
            Text(
                text = "Conexiones: ${node.connections.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyGraphState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🕸️",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "No hay elementos para mostrar en el grafo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Agrega clases, propiedades o individuos para ver la visualización",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Data classes and enums
enum class NodeType(val displayName: String) {
    CLASS("Clase"),
    OBJECT_PROPERTY("Propiedad de Objeto"),
    DATA_PROPERTY("Propiedad de Datos"),
    INDIVIDUAL("Individuo"),
    METAMODEL("Metamodelado")
}

data class GraphNode(
    val id: String,
    val label: String,
    val type: NodeType,
    var position: Offset = Offset.Zero,
    val connections: MutableList<String> = mutableListOf()
)

data class GraphEdge(
    val from: String,
    val to: String,
    val type: EdgeType = EdgeType.SUBCLASS,
    val dashed: Boolean = false,
    val color: Color = HermitColors.GraphEdge
)

enum class EdgeType {
    SUBCLASS,
    PROPERTY_DOMAIN,
    PROPERTY_RANGE,
    INSTANCE_OF
}

data class GraphData(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val clusters: List<GraphCluster>
)

data class GraphCluster(
    val label: String,
    val memberIds: Set<String>,
    val fillColor: Color,
    val borderColor: Color
)

// Graph generation and layout functions
fun generateGraphData(
    classes: List<String>,
    objectProperties: List<String>,
    dataProperties: List<String>,
    individuals: List<String>,
    subClassRelations: List<SubClassRelation>
): GraphData {
    val nodes = mutableListOf<GraphNode>()
    val edges = mutableListOf<GraphEdge>()
    val clusters = mutableListOf<GraphCluster>()
    
    // Add class nodes
    classes.forEach { className ->
        nodes.add(GraphNode(
            id = "class_$className",
            label = className,
            type = if (className.contains("Meta") || className.contains("meta")) 
                NodeType.METAMODEL else NodeType.CLASS
        ))
    }
    
    // Add property nodes
    objectProperties.forEach { prop ->
        nodes.add(GraphNode(
            id = "objprop_$prop",
            label = prop,
            type = NodeType.OBJECT_PROPERTY
        ))
    }
    
    dataProperties.forEach { prop ->
        nodes.add(GraphNode(
            id = "dataprop_$prop",
            label = prop,
            type = NodeType.DATA_PROPERTY
        ))
    }
    
    // Add individual nodes
    individuals.forEach { individual ->
        nodes.add(GraphNode(
            id = "individual_$individual",
            label = individual,
            type = NodeType.INDIVIDUAL
        ))
    }
    
    // Add subclass edges
    subClassRelations.forEach { relation ->
        val fromNode = nodes.find { it.label == relation.subClass }
        val toNode = nodes.find { it.label == relation.superClass }
        
        if (fromNode != null && toNode != null) {
            edges.add(GraphEdge(fromNode.id, toNode.id, EdgeType.SUBCLASS))
            fromNode.connections.add(toNode.id)
            toNode.connections.add(fromNode.id)
        }
    }
    
    // Heuristic clusters: detect superclasses with many direct subclasses
    val childrenByParent: Map<String, List<String>> = subClassRelations
        .groupBy { it.superClass }
        .mapValues { entry -> entry.value.map { it.subClass } }
    
    childrenByParent.forEach { (parent, children) ->
        if (children.size >= 4) {
            val memberIds = children.mapNotNull { child -> nodes.find { it.label == child }?.id }.toSet()
            if (memberIds.isNotEmpty()) {
                val colors = clusterColorsForLabel(parent)
                clusters.add(
                    GraphCluster(
                        label = parent,
                        memberIds = memberIds,
                        fillColor = colors.first.copy(alpha = 0.25f),
                        borderColor = colors.second
                    )
                )
                // Emphasize edges from parent to members as dashed red when applicable
                val parentNode = nodes.find { it.label == parent }
                if (parentNode != null) {
                    children.forEach { child ->
                        val childNode = nodes.find { it.label == child }
                        if (childNode != null) {
                            edges.add(
                                GraphEdge(
                                    from = nodes.find { it.label == child }!!.id,
                                    to = parentNode.id,
                                    type = EdgeType.SUBCLASS,
                                    dashed = isRedDashedCluster(parent),
                                    color = if (isRedDashedCluster(parent)) Color.Red else HermitColors.GraphEdge
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    
    return GraphData(nodes, edges, clusters)
}

fun updateNodePositions(nodes: List<GraphNode>, width: Float, height: Float) {
    val centerX = width / 2
    val centerY = height / 2
    val margin = 50f
    
    // Group nodes by type for better layout
    val classNodes = nodes.filter { it.type == NodeType.CLASS || it.type == NodeType.METAMODEL }
    val propertyNodes = nodes.filter { it.type == NodeType.OBJECT_PROPERTY || it.type == NodeType.DATA_PROPERTY }
    val individualNodes = nodes.filter { it.type == NodeType.INDIVIDUAL }
    
    // Layout classes in the center
    if (classNodes.isNotEmpty()) {
        val classRadius = minOf(width, height) / 4
        classNodes.forEachIndexed { index, node ->
            val angle = 2 * PI * index / classNodes.size
            node.position = Offset(
                x = (centerX + classRadius * cos(angle)).toFloat(),
                y = (centerY + classRadius * sin(angle)).toFloat()
            )
        }
    }
    
    // Layout properties around the outside
    if (propertyNodes.isNotEmpty()) {
        val propRadius = minOf(width, height) / 2.5f
        propertyNodes.forEachIndexed { index, node ->
            val angle = 2 * PI * index / propertyNodes.size
            node.position = Offset(
                x = (centerX + propRadius * cos(angle)).toFloat(),
                y = (centerY + propRadius * sin(angle)).toFloat()
            )
        }
    }
    
    // Layout individuals at the bottom
    if (individualNodes.isNotEmpty()) {
        val startX = margin
        val endX = width - margin
        val y = height - margin
        val spacing = if (individualNodes.size > 1) (endX - startX) / (individualNodes.size - 1) else 0f
        
        individualNodes.forEachIndexed { index, node ->
            node.position = Offset(
                x = startX + index * spacing,
                y = y
            )
        }
    }
}

// Drawing functions
fun DrawScope.drawNode(node: GraphNode) {
    val color = when (node.type) {
        NodeType.CLASS -> HermitColors.Primary
        NodeType.OBJECT_PROPERTY -> HermitColors.Secondary
        NodeType.DATA_PROPERTY -> HermitColors.Tertiary
        NodeType.INDIVIDUAL -> HermitColors.Error
        NodeType.METAMODEL -> Color(0xFF9C27B0)
    }
    
    val nodeRadius = when (node.type) {
        NodeType.CLASS, NodeType.METAMODEL -> 25f
        NodeType.OBJECT_PROPERTY, NodeType.DATA_PROPERTY -> 20f
        NodeType.INDIVIDUAL -> 15f
    }
    
    // Draw node background
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = nodeRadius,
        center = node.position
    )
    
    // Draw node border
    drawCircle(
        color = color,
        radius = nodeRadius,
        center = node.position,
        style = Stroke(width = 3f)
    )
    
    // Special shape for individuals (square)
    if (node.type == NodeType.INDIVIDUAL) {
        val size = nodeRadius * 1.5f
        drawRect(
            color = color.copy(alpha = 0.3f),
            topLeft = Offset(
                node.position.x - size/2,
                node.position.y - size/2
            ),
            size = androidx.compose.ui.geometry.Size(size, size)
        )
        drawRect(
            color = color,
            topLeft = Offset(
                node.position.x - size/2,
                node.position.y - size/2
            ),
            size = androidx.compose.ui.geometry.Size(size, size),
            style = Stroke(width = 3f)
        )
    }
}

fun DrawScope.drawEdge(startPos: Offset, endPos: Offset, color: Color, dashed: Boolean) {
    drawLine(
        color = color,
        start = startPos,
        end = endPos,
        strokeWidth = 2f,
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f) else null
    )
    
    // Draw arrow head
    val angle = atan2(endPos.y - startPos.y, endPos.x - startPos.x)
    val arrowLength = 15f
    val arrowAngle = 0.5f
    
    val arrowPoint1 = Offset(
        endPos.x - arrowLength * cos(angle - arrowAngle),
        endPos.y - arrowLength * sin(angle - arrowAngle)
    )
    val arrowPoint2 = Offset(
        endPos.x - arrowLength * cos(angle + arrowAngle),
        endPos.y - arrowLength * sin(angle + arrowAngle)
    )
    
    drawLine(color = color, start = endPos, end = arrowPoint1, strokeWidth = 2f)
    drawLine(color = color, start = endPos, end = arrowPoint2, strokeWidth = 2f)
}

fun DrawScope.drawNodeLabel(
    node: GraphNode,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    style: TextStyle
) {
    if (node.position == Offset.Zero) return
    val text = AnnotatedString(node.label)
    val layout = textMeasurer.measure(text, style)
    val textWidth = layout.size.width.toFloat()
    val textHeight = layout.size.height.toFloat()
    val offset = Offset(
        x = node.position.x - textWidth / 2,
        y = node.position.y - textHeight / 2
    )
    drawText(
        textLayoutResult = layout,
        topLeft = offset,
        color = Color.Black
    )
}

fun DrawScope.drawCluster(cluster: GraphCluster, nodes: List<GraphNode>) {
    val (topLeft, size) = computeClusterBounds(cluster, nodes)
    val corner = 60f
    drawRoundRect(
        color = cluster.fillColor,
        topLeft = topLeft,
        size = androidx.compose.ui.geometry.Size(size.x, size.y),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
    )
    drawRoundRect(
        color = cluster.borderColor,
        topLeft = topLeft,
        size = androidx.compose.ui.geometry.Size(size.x, size.y),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
        style = Stroke(width = 3f)
    )
}

fun computeClusterBounds(cluster: GraphCluster, nodes: List<GraphNode>): Pair<Offset, Offset> {
    val members = nodes.filter { cluster.memberIds.contains(it.id) && it.position != Offset.Zero }
    if (members.isEmpty()) return Pair(Offset.Zero, Offset.Zero)
    val padding = 60f
    val minX = members.minOf { it.position.x } - padding
    val maxX = members.maxOf { it.position.x } + padding
    val minY = members.minOf { it.position.y } - padding
    val maxY = members.maxOf { it.position.y } + padding
    return Pair(Offset(minX, minY), Offset(maxX - minX, maxY - minY))
}

@Composable
fun ClusterLabel(text: String, topLeft: Offset, size: Offset) {
    if (size.x <= 0f || size.y <= 0f) return
    Box(
        modifier = Modifier
            .offset(x = (topLeft.x * 0.8f).dp, y = (topLeft.y * 0.8f - 18).dp)
            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

private fun clusterColorsForLabel(label: String): Pair<Color, Color> {
    return when {
        label.contains("risk", true) || label.contains("riesgo", true) -> Pair(Color(0xFFFFCDD2), Color.Red)
        label.contains("history", true) || label.contains("historia", true) -> Pair(Color(0xFFBBDEFB), Color.Red)
        label.contains("model", true) || label.contains("modelo", true) -> Pair(Color(0xFFFFCDD2), Color.Red)
        label.contains("recommendation", true) || label.contains("recomend", true) || label.contains("recomendación", true) -> Pair(Color(0xFFC8E6C9), Color(0xFF2E7D32))
        else -> Pair(HermitColors.SurfaceVariant, Color(0xFFBDBDBD))
    }
}

private fun isRedDashedCluster(label: String): Boolean {
    return label.contains("risk", true) || label.contains("riesgo", true) ||
           label.contains("history", true) || label.contains("historia", true)
}

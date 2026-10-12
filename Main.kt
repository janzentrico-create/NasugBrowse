abstract class Destination(
    val id: Int,
    val name: String,
    val location: String,
    open val baseCost: Double
) {
    open fun describe(): String =
        "[$id] $name ($location) - ₱${"%.2f".format(baseCost)}"

    open fun category(): String = "General"
}

class Beach(
    id: Int,
    name: String,
    location: String,
    baseCost: Double,
    val hasResort: Boolean
) : Destination(id, name, location, baseCost) {

    override fun category(): String = "Beach"

    override fun describe(): String =
        super.describe() + " | Resort: ${if (hasResort) "Yes" else "No"}"
}

class HistoricalSite(
    id: Int,
    name: String,
    location: String,
    baseCost: Double,
    val entryFee: Double
) : Destination(id, name, location, baseCost) {

    override val baseCost: Double
        get() = super.baseCost + entryFee

    override fun category(): String = "Historical Site"
}

class Restaurant(
    id: Int,
    name: String,
    location: String,
    baseCost: Double,
    val cuisine: String
) : Destination(id, name, location, baseCost) {

    override fun category(): String = "Restaurant"

    override fun describe(): String =
        super.describe() + " | Cuisine: $cuisine"
}

/** Encapsulates a user's itinerary. */
class Itinerary(val ownerName: String) {

    private val items = mutableListOf<Destination>()

    fun addDestination(destination: Destination): Boolean {
        if (items.any { it.id == destination.id }) return false
        items.add(destination)
        return true
    }

    fun removeDestination(id: Int): Boolean =
        items.removeIf { it.id == id }

    fun listDestinations(): List<Destination> = items.toList()

    fun totalCost(): Double = items.sumOf { it.baseCost }

    fun summary(): String {
        if (items.isEmpty()) return "Itinerary is empty."

        val sb = StringBuilder()
        sb.appendLine("Itinerary for: $ownerName")
        sb.appendLine("-".repeat(40))
        items.forEachIndexed { index, dest ->
            sb.appendLine("${index + 1}. ${dest.describe()}")
        }
        sb.appendLine("-".repeat(40))
        sb.appendLine("Total Estimated Cost: ₱${"%.2f".format(totalCost())}")
        return sb.toString()
    }
}

// ============================================================
// SERVICE LAYER
// ============================================================

/** Handles destination catalog + selection logic. */
class PlannerService {

    private val destinations = mutableListOf<Destination>()

    fun addDestination(destination: Destination) {
        destinations.add(destination)
    }

    fun getAllDestinations(): List<Destination> = destinations.toList()

    fun filterByCategory(category: String): List<Destination> =
        destinations.filter { it.category().equals(category, ignoreCase = true) }

    fun filterByMaxCost(maxCost: Double): List<Destination> =
        destinations.filter { it.baseCost <= maxCost }

    fun searchByName(keyword: String): List<Destination> =
        destinations.filter { it.name.contains(keyword, ignoreCase = true) }
}

// ============================================================
// MAIN — DEMO
// ============================================================

fun main() {
    val planner = PlannerService()

    // Seed sample destinations
    planner.addDestination(Beach(1, "Canyon Cove Hotel & Spa", "Wawa, Nasugbu, Batangas", 4113.0, true))
    planner.addDestination(Beach(2, "Pamana Resorts and Hotels", "Calayo, Nasugbu, Batangas", 4076.0, true))
    planner.addDestination(HistoricalSite(3, "La Iglesia de la Nuestra Señora de la Escalera", "Brgy.6, Nasugbu Batangas", 200.0, 75.0))
    planner.addDestination(Restaurant(4, "Kainan sa Dalampasigan", "Bucana, Nasugbu, Batangas", 650.0, "Filipino"))
    planner.addDestination(HistoricalSite(5, "Caleruega Chapel of Transfiguration", "Batulao, Nasugbu, Batangas", 500.0, 0.0))

    println("=== ALL DESTINATIONS ===")
    planner.getAllDestinations().forEach { println(it.describe()) }

    println("\n=== FILTER: Beaches ===")
    planner.filterByCategory("Beach").forEach { println(it.describe()) }

    println("\n=== FILTER: Max Cost ₱1000 ===")
    planner.filterByMaxCost(5000.0).forEach { println(it.describe()) }

    println("\n=== SEARCH: 'Vigan' ===")
    planner.searchByName("Vigan").forEach { println(it.describe()) }

    // Build an itinerary
    val itinerary = Itinerary("Juan Dela Cruz")
    itinerary.addDestination(planner.getAllDestinations()[0])
    itinerary.addDestination(planner.getAllDestinations()[2])
    itinerary.addDestination(planner.getAllDestinations()[4])

    println("\n=== ITINERARY SUMMARY ===")
    println(itinerary.summary())
}
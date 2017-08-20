package com.github.giedomak.telepathdb.datamodels.parsetree

import com.github.giedomak.telepathdb.TelepathDB
import com.github.giedomak.telepathdb.datamodels.Edge
import com.github.giedomak.telepathdb.datamodels.Query
import com.github.giedomak.telepathdb.datamodels.parsetree.PhysicalPlan.Companion.HASHJOIN
import com.github.giedomak.telepathdb.datamodels.stores.PathIdentifierStore
import com.github.giedomak.telepathdb.memorymanager.MemoryManager

/**
 * Representation of the physical plan.
 *
 * This class extends MultiTree and and has physical operators instread of logical operators in comparison
 * to the [ParseTree] class.
 *
 * @property query We always need a reference to our query which holds all the module implementations we'll need.
 * @property operator An [Int] representing the physical operator. See [HASHJOIN] for an example.
 * @property operatorName This property gets the symbolic name [String] belonging to our operator, e.g. HASHJOIN.
 * @property memoryManagerId This property will hold a reference to the [MemoryManager] slot we might be given
 *                           to indicate the location of intermediate results.
 */
class PhysicalPlan(
        query: Query,
        override var operator: Int = 0
) : MultiTree<PhysicalPlan>(query) {

    override val operatorName get() = SYMBOLIC_NAMES[operator]
    var memoryManagerId = -1L

    /**
     * Directly construct a PhysicalPlan for the given [leaf].
     *
     * @param query We always need a reference to a query.
     * @param leaf The [Edge] which will be the leaf of this ParseTree.
     */
    constructor(query: Query, leaf: Edge) : this(query, INDEXLOOKUP) {
        // Create and set the child-leaf as a child of our INDEXLOOKUP
        val child = PhysicalPlan(query)
        child.leaf = leaf
        this.children.add(child)
    }

    fun pathIdOfChildren(): Long {
        return PathIdentifierStore.getPathIdByEdges(children.map { it.leaf!! })
    }

    fun cardinality(): Long {
        return query.telepathDB.cardinalityEstimation.getCardinality(this)
    }

    fun cost(): Long {
        return TelepathDB.costModel.cost(this)
    }

    fun merge(tree: PhysicalPlan, operator: Int): PhysicalPlan {
        val root = PhysicalPlan(query, operator)
        root.children.add(this.clone())
        root.children.add(tree.clone())
        return root
    }

    companion object {

        const val INDEXLOOKUP = 1
        const val HASHJOIN = 2
        const val NESTEDLOOPJOIN = 3

        private val SYMBOLIC_NAMES = arrayOf(null, "INDEXLOOKUP", "HASHJOIN", "NESTEDLOOPJOIN")

    }
}
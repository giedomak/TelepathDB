package com.github.giedomak.telepathdb.datamodels.utilities

import com.github.giedomak.telepathdb.datamodels.ParseTree
import com.github.giedomak.telepathdb.staticparser.StaticParserRPQTest
import org.junit.Test
import kotlin.test.assertEquals

class UnionPullerTest {

    @Test
    fun pullsOutUnionsIntoMultipleParseTrees() {

        // Given
        val input = exampleUnionParseTree()
        val actual = UnionPuller.parse(input)

        // Create expected parseTree
        //       CONCATENATION
        //        /      \
        //  CONCATENATION  d
        //      /   \
        //     a     b
        val child1 = StaticParserRPQTest.create1LevelParseTree(
                ParseTree.CONCATENATION, listOf("a", "b"), false)
        val root1 = StaticParserRPQTest.create1LevelParseTree(
                ParseTree.CONCATENATION, listOf("d"))
        root1.children.add(0, child1)

        // Create expected parseTree
        //       CONCATENATION
        //        /      \
        //  CONCATENATION  d
        //      /   \
        //     a     c
        val child2 = StaticParserRPQTest.create1LevelParseTree(
                ParseTree.CONCATENATION, listOf("a", "c"), false)
        val root2 = StaticParserRPQTest.create1LevelParseTree(
                ParseTree.CONCATENATION, listOf("d"))
        root2.children.add(0, child2)

        assertEquals(listOf(root1, root2), actual)
    }

    @Test
    fun splitsParseTreesWhenRootIsUnion() {

        // Given:
        //     UNION
        //      / \
        //     a   b
        val input = StaticParserRPQTest.create1LevelParseTree(ParseTree.UNION, listOf("a", "b"))
        val actual = UnionPuller.parse(input)

        // Generate expected
        val a = StaticParserRPQTest.createSimpleParseTree("a")
        val b = StaticParserRPQTest.createSimpleParseTree("b")

        assertEquals(listOf(a, b), actual)
    }

    private fun exampleUnionParseTree(): ParseTree {

        // Your input: a/(b|c)/d
        //
        //        CONCATENATION[2]
        //       / \
        //      /   \
        //     /     \
        //    /       \
        //    CONCATENATION[2]       d
        //   / \
        //  /   \
        //  a   UNION[2]
        //       / \
        //       b c
        val child2 = StaticParserRPQTest.create1LevelParseTree(
                ParseTree.UNION, listOf("b", "c"), false)
        val child1 = StaticParserRPQTest.create1LevelParseTree(
                ParseTree.CONCATENATION, listOf("a"), false)
        child1.setChild(1, child2)

        val root = StaticParserRPQTest.create1LevelParseTree(ParseTree.CONCATENATION, listOf("d"))
        root.children.add(0, child1)

        return root
    }
}
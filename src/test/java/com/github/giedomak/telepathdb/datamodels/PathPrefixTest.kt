/**
 * Copyright (C) 2016-2017 - All rights reserved.
 * This file is part of the telepathdb project which is released under the GPLv3 license.
 * See file LICENSE.txt or go to http://www.gnu.org/licenses/gpl.txt for full license details.
 * You may use, distribute and modify this code under the terms of the GPLv3 license.
 */

package com.github.giedomak.telepathdb.datamodels

import com.github.giedomak.telepathdb.datamodels.stores.PathIdentifierStore
import junit.framework.TestCase.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.*

class PathPrefixTest {

    @Test
    fun samePathPrefixPrefixesEqualEachOtherTest() {
        // given
        val pathId = PathIdentifierStore.getPathIdByEdgeLabel(Arrays.asList("a", "b", "c"))
        val a = PathPrefix(pathId, 6, PathTest.equalNodes(4, 42))
        val b = PathPrefix(pathId, 6, PathTest.equalNodes(4, 42))

        // then
        assertEquals(a, a)
        assertEquals(a, b)
    }

    @Test
    fun differentPathPrefixPrefixesAreNotEqualsTest() {
        // given
        val a = PathPrefix(42, 6, PathTest.equalNodes(4, 42))
        val b = PathPrefix(42, 6, PathTest.equalNodes(4, 24))
        val c = PathPrefix(42, 6, PathTest.equalNodes(3, 42))

        val differentNodes = PathTest.equalNodes(3, 42)
        differentNodes.removeAt(differentNodes.size - 1)
        differentNodes.add(Node(43))

        val d = PathPrefix(42, 6, differentNodes)

        // then
        assertFalse(a == b)
        assertFalse(b == a)
        assertFalse(a == c)
        assertFalse(c == a)
        assertFalse(c == d)
        assertFalse(d == c)
    }

    // ---------- METHODS ---------

    @Test
    fun generatesHashCode() {
        // given
        val a = PathPrefix(42, 6, PathTest.equalNodes(4, 42))
        val b = PathPrefix(42, 6, PathTest.equalNodes(4, 42))
        val c = PathPrefix(42, 6, PathTest.equalNodes(4, 24))

        // then
        assertEquals(a.hashCode().toLong(), b.hashCode().toLong())
        assertNotEquals(a.hashCode().toLong(), c.hashCode().toLong())
    }

    @Test
    fun outputsToString() {
        // given
        val pathId = PathIdentifierStore.getPathIdByEdgeLabel(Arrays.asList("a", "b", "c"))
        val a = PathPrefix(pathId)
        val output = "PathPrefix(" + "pathId=" + a.pathId + ", length=" + a.length + ", edges=" + PathIdentifierStore.getEdgeSet(pathId) + ", nodes=" + a.nodes + ")"

        // then
        assertEquals(a.toString(), output)
    }

    companion object {

        fun simplePathPrefix(pathId: Long, actualLength: Int, numberOfNodes: Int, value: Long?): PathPrefix {
            val nodes = ArrayList<Node>(numberOfNodes + 1)

            for (i in 0..numberOfNodes - 1) {
                nodes.add(Node(value!!))
            }

            return PathPrefix(pathId, actualLength, nodes)
        }
    }
}
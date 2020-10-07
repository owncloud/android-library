/* ownCloud Android Library is available under MIT license
*   Copyright (C) 2020 ownCloud GmbH.
*
*   Permission is hereby granted, free of charge, to any person obtaining a copy
*   of this software and associated documentation files (the "Software"), to deal
*   in the Software without restriction, including without limitation the rights
*   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
*   copies of the Software, and to permit persons to whom the Software is
*   furnished to do so, subject to the following conditions:
*
*   The above copyright notice and this permission notice shall be included in
*   all copies or substantial portions of the Software.
*
*   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
*   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
*   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
*   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
*   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
*   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
*   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
*   THE SOFTWARE.
*
*/

package com.owncloud.android.lib

import com.owncloud.android.lib.resources.status.StatusRequester
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusRequestorTest {
    private val requestor = StatusRequester()

    @Test
    fun testUpdateLocationWithAnAbsolutePath() {
        val newLocation = requestor.updateLocationWithRedirectPath(TEST_DOMAIN, "$TEST_DOMAIN$SUB_PATH")
        assertEquals("$TEST_DOMAIN$SUB_PATH", newLocation)
    }

    @Test
    fun updateLocationWithASmallerAbsolutePath() {
        val newLocation = requestor.updateLocationWithRedirectPath("$TEST_DOMAIN$SUB_PATH", TEST_DOMAIN)
        assertEquals(TEST_DOMAIN, newLocation)
    }

    @Test
    fun updateLocationWithARelativePath() {
        val newLocation = requestor.updateLocationWithRedirectPath(TEST_DOMAIN, SUB_PATH)
        assertEquals("$TEST_DOMAIN$SUB_PATH", newLocation)
    }

    @Test
    fun updateLocationByReplacingTheRelativePath() {
        val newLocation = requestor.updateLocationWithRedirectPath(
            "$TEST_DOMAIN/some/other/subdir", SUB_PATH
        )
        assertEquals("$TEST_DOMAIN$SUB_PATH", newLocation)
    }

    companion object {
        const val TEST_DOMAIN = "https://cloud.somewhere.com"
        const val SUB_PATH = "/subdir"
    }
}

/**
 * Copyright © 2018 Reijhanniel Jearl Campos (devcsrj@torocloud.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mazerunner

import mazerunner.MazeMovementWebSocketHandler.Companion.extractPoint
import org.junit.Test
import kotlin.test.assertEquals

class MazeMovementWebSocketHandlerTest {

    @Test
    fun `can transform coordinates to Points`() {
        var actual = extractPoint("(0,0)")
        assertEquals(actual, Point(0, 0))

        actual = extractPoint("(0,1)")
        assertEquals(actual, Point(0, 1))

        actual = extractPoint("(1,1)")
        assertEquals(actual, Point(1, 1))

        actual = extractPoint("(-1,-1)")
        assertEquals(actual, Point(-1, -1))
    }

}

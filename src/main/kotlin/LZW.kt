
import java.io.*


object LZW {
    /**
     * Compress by LZW
     *
     * @param uncompress
     * @return compressed
     */
    fun compress(uncompress: ByteArray):ByteArray {
        val byte = (ByteArrayOutputStream(uncompress.size))
        val out  = DataOutputStream(BufferedOutputStream(byte))
        val read = DataInputStream(BufferedInputStream(ByteArrayInputStream(uncompress)))
        /** Convert 8 bit to 12 bit  */
        fun to12bit(i: Int): String {
            var temp = Integer.toBinaryString(i)
            while (temp.length < 12) {
                temp = "0$temp"
            }
            return temp
        }

        /** Local Variables  */
        var inputByte: Byte
        val charArray = arrayOfNulls<String>(4096)
        val table = HashMap<String, Int>()
        for (i in 0..255) {
            table[i.toChar().toString()] = i
            charArray[i] = i.toChar().toString()
        }
        var count = 256
        var temp = ""
        val buffer = ByteArray(3)
        var onleft = true
        try {
            /** Read the First Character from input file into the String  */
            inputByte = read.readByte()
            var i = inputByte.toInt()
            if (i < 0) {
                i += 256
            }
            var c = i.toChar()
            temp = "" + c
            /** Read Character by Character  */
            while (true) {
                inputByte = read.readByte()
                i = inputByte.toInt()
                if (i < 0) {
                    i += 256
                }
                c = i.toChar()
                if (table.containsKey(temp + c)) {
                    temp += c
                } else {
                    val s12 = to12bit(table[temp]!!)
                    /**
                     * Store the 12 bits into an array and then write it to the
                     * output file
                     */
                    if (onleft) {
                        buffer[0] =
                            s12.substring(0, 8).toInt(2).toByte()
                        buffer[1] = (
                                s12.substring(8, 12) + "0000").toInt(2).toByte()
                    } else {
                        buffer[1] = buffer[1].plus(s12.substring(0, 4).toInt(2).toByte()).toByte()
                        buffer[2] = s12.substring(4, 12).toInt(2).toByte()
                        for (b in buffer.indices) {
                            out.writeByte(buffer[b].toInt())
                            buffer[b] = 0
                        }
                    }
                    onleft = !onleft
                    if (count < 4096) {
                        table[temp + c] = count++
                    }
                    temp = "" + c
                }
            }
        } catch (e: EOFException) {
            val temp12 = to12bit(table[temp]!!)
            if (onleft) {
                buffer[0] = temp12.substring(0, 8).toInt(2).toByte()
                buffer[1] = (temp12.substring(8, 12)
                        + "0000").toInt(2).toByte()
                out.writeByte(buffer[0].toInt())
                out.writeByte(buffer[1].toInt())
            } else {
                buffer[1] = (buffer[1] + temp12.substring(0, 4).toInt(2).toByte()).toByte()
                buffer[2] = temp12.substring(4, 12).toInt(2).toByte()
                var b = 0
                while (b < buffer.size) {
                    out.writeByte(buffer[b].toInt())
                    buffer[b] = 0
                    b++
                }
            }
            read.close()
            out.close()
        }
        return byte.toByteArray()
    }

    /**
     * Decompress by LZW
     *
     * @param compressed
     * @return uncompress
     */
    fun decompress(compressed:ByteArray):ByteArray {
        /**
         * Extract the 12 bit key from 2 bytes and get the int value of the key
         *
         * @param b1
         * @param b2
         * @param onLeft
         * @return an Integer which holds the value of the key
         */
        fun getValue(b1: Byte, b2: Byte, onLeft: Boolean): Int {
            var temp1 = Integer.toBinaryString(b1.toInt())
            var temp2 = Integer.toBinaryString(b2.toInt())
            while (temp1.length < 8) {
                temp1 = "0$temp1"
            }
            if (temp1.length == 32) {
                temp1 = temp1.substring(24, 32)
            }
            while (temp2.length < 8) {
                temp2 = "0$temp2"
            }
            if (temp2.length == 32) {
                temp2 = temp2.substring(24, 32)
            }
            /** On left being true  */
            return if (onLeft) {
                (temp1 + temp2.substring(0, 4)).toInt(2)
            } else {
                (temp1.substring(4, 8) + temp2).toInt(2)
            }
        }
        val byte = ByteArrayOutputStream()
        val out = DataOutputStream(BufferedOutputStream(byte))
        val inputStream = DataInputStream(BufferedInputStream(ByteArrayInputStream(compressed)))
        val charArray = arrayOfNulls<String>(4096)
        val table = HashMap<String, Int>()
        for (i in 0..255) {
            table[i.toChar().toString()] = i
            charArray[i] = i.toChar().toString()
        }
        var count = 256
        var currword: Int
        var priorword: Int
        val buffer = ByteArray(3)
        var onleft = true
        try {
            /**
             * Get the first word in code and output its corresponding character
             */
            buffer[0] = inputStream.readByte()
            buffer[1] = inputStream.readByte()
            priorword = getValue(buffer[0], buffer[1], onleft)
            onleft = !onleft
            out.writeBytes(charArray[priorword]!!)
            /**
             * Read every 3 bytes and generate a corresponding characters - 2
             * character
             */
            while (true) {
                if (onleft) {
                    buffer[0] = inputStream.readByte()
                    buffer[1] = inputStream.readByte()
                    currword = getValue(buffer[0], buffer[1], onleft)
                } else {
                    buffer[2] = inputStream.readByte()
                    currword = getValue(buffer[1], buffer[2], onleft)
                }
                onleft = !onleft
                if (currword >= count) {
                    if (count < 4096) charArray[count] = (charArray[priorword]
                            + charArray[priorword]!![0])
                    count++
                    out.writeBytes(
                        charArray[priorword]
                                + charArray[priorword]!![0]
                    )
                } else {
                    if (count < 4096) charArray[count] = (charArray[priorword]
                            + charArray[currword]!![0])
                    count++
                    out.writeBytes(charArray[currword]!!)
                }
                priorword = currword
            }
        } catch (e: EOFException) {
            inputStream.close()
            out.close()
        }
        return byte.toByteArray()
    }

}



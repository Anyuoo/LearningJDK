/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.nio;

/**
 * A read/write HeapLongBuffer.
 */

// 可读写、非直接缓冲区，内部存储结构实现为long[]
class HeapLongBuffer extends LongBuffer {
    
    // Cached array base offset
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(long[].class);
    // Cached array base offset
    private static final long ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale(long[].class);
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    protected HeapLongBuffer(long[] buf, int mark, int pos, int lim, int cap, int off) {
        super(mark, pos, lim, cap, buf, off);
        this.address = ARRAY_BASE_OFFSET + off * ARRAY_INDEX_SCALE;
    }
    
    HeapLongBuffer(int cap, int lim) {            // package-private
        super(-1, 0, lim, cap, new long[cap], 0);
        this.address = ARRAY_BASE_OFFSET;
    }
    
    HeapLongBuffer(long[] buf, int off, int len) { // package-private
        super(-1, off, off + len, buf.length, buf, 0);
        this.address = ARRAY_BASE_OFFSET;
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 可读写/非直接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public boolean isReadOnly() {
        return false;
    }
    
    public boolean isDirect() {
        return false;
    }
    
    /*▲ 可读写/非直接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public LongBuffer slice() {
        return new HeapLongBuffer(hb, -1, 0, this.remaining(), this.remaining(), this.position() + offset);
    }
    
    public LongBuffer duplicate() {
        return new HeapLongBuffer(hb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
    }
    
    public LongBuffer asReadOnlyBuffer() {
        return new HeapLongBufferR(hb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get/读取 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public long get() {
        return hb[ix(nextGetIndex())];
    }
    
    public long get(int i) {
        return hb[ix(checkIndex(i))];
    }
    
    public LongBuffer get(long[] dst, int offset, int length) {
        checkBounds(offset, length, dst.length);
        if(length>remaining())
            throw new BufferUnderflowException();
        System.arraycopy(hb, ix(position()), dst, offset, length);
        position(position() + length);
        return this;
    }
    
    /*▲ get/读取 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ put/写入 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public LongBuffer put(long x) {
        hb[ix(nextPutIndex())] = x;
        return this;
    }
    
    public LongBuffer put(int i, long x) {
        hb[ix(checkIndex(i))] = x;
        return this;
    }
    
    public LongBuffer put(long[] src, int offset, int length) {
        checkBounds(offset, length, src.length);
        if(length>remaining())
            throw new BufferOverflowException();
        System.arraycopy(src, offset, hb, ix(position()), length);
        position(position() + length);
        return this;
    }
    
    public LongBuffer put(LongBuffer src) {
        if(src instanceof HeapLongBuffer) {
            if(src == this)
                throw createSameBufferException();
            HeapLongBuffer sb = (HeapLongBuffer) src;
            int n = sb.remaining();
            if(n>remaining())
                throw new BufferOverflowException();
            System.arraycopy(sb.hb, sb.ix(sb.position()), hb, ix(position()), n);
            sb.position(sb.position() + n);
            position(position() + n);
        } else if(src.isDirect()) {
            int n = src.remaining();
            if(n>remaining())
                throw new BufferOverflowException();
            src.get(hb, ix(position()), n);
            position(position() + n);
        } else {
            super.put(src);
        }
        return this;
    }
    
    /*▲ put/写入 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public LongBuffer compact() {
        System.arraycopy(hb, ix(position()), hb, ix(0), remaining());
        position(remaining());
        limit(capacity());
        discardMark();
        return this;
    }
    
    /*▲ 压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }
    
    /*▲ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    protected int ix(int i) {
        return i + offset;
    }
}

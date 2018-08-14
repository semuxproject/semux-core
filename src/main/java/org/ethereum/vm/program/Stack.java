/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.vm.program;

import org.ethereum.vm.DataWord;

/**
 * Program runtime stack.
 */
public class Stack {

    private java.util.Stack<DataWord> stack = new java.util.Stack<>();

    public synchronized DataWord pop() {
        return stack.pop();
    }

    public void push(DataWord item) {
        stack.push(item);
    }

    public void swap(int from, int to) {
        if (isAccessible(from) && isAccessible(to) && (from != to)) {
            DataWord tmp = stack.get(from);
            stack.set(from, stack.set(to, tmp));
        }
    }

    public DataWord peek() {
        return stack.peek();
    }

    public DataWord get(int index) {
        return stack.get(index);
    }

    public int size() {
        return stack.size();
    }

    private boolean isAccessible(int from) {
        return from >= 0 && from < stack.size();
    }

    public DataWord[] toArray() {
        return stack.toArray(new DataWord[0]);
    }
}

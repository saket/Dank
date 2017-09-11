/*
 * Copyright (C) 2015-2017 Emanuel Moecklin
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

package com.onegravity.rteditor;

import android.annotation.SuppressLint;
import android.text.Spannable;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * This class manages Operations for multiple rich text editors.
 * It's used by the RTManager to undo/redo operations.
 */
@SuppressLint("UseSparseArrays")
class RTOperationManager {

    /*
     * Maximum number of operations to put in the undo/redo stack
     */
    private static final int MAX_NR_OF_OPERATIONS = 50;

    /*
     * two operations performed in this time frame (in ms) are considered one
     * operation
     */
    private static final int TIME_BETWEEN_OPERATIONS = 300;

    /*
     * The undo/redo stacks by editor Id
     */
    private Map<Integer, Stack<Operation>> mUndoStacks = new HashMap<Integer, Stack<Operation>>();
    private Map<Integer, Stack<Operation>> mRedoStacks = new HashMap<Integer, Stack<Operation>>();
    ;

    // ****************************************** Operation Classes *******************************************

    /**
     * An atomic operation in the rich text editor.
     * If two operations are performed within a certain time frame
     * they will be considered as one operation and un-done/re-done together.
     */
    private abstract static class Operation {
        protected long mTimestamp;

        private int mSelStartBefore;
        private int mSelEndBefore;
        private Spannable mBefore;

        private int mSelStartAfter;
        private int mSelEndAfter;
        private Spannable mAfter;

        Operation(Spannable before, Spannable after, int selStartBefore, int selEndBefore, int selStartAfter, int selEndAfter) {
            mSelStartBefore = selStartBefore;
            mSelEndBefore = selEndBefore;
            mSelStartAfter = selStartAfter;
            mSelEndAfter = selEndAfter;
            mBefore = before;
            mAfter = after;
            mTimestamp = System.currentTimeMillis();
        }

        boolean canMerge(Operation other) {
            return Math.abs(mTimestamp - other.mTimestamp) < TIME_BETWEEN_OPERATIONS;
        }

        Operation merge(Operation previousOp) {
            mBefore = previousOp.mBefore;
            mSelStartBefore = previousOp.mSelStartBefore;
            mSelEndBefore = previousOp.mSelEndBefore;
            return this;
        }

        void undo(RTEditText editor) {
            editor.ignoreTextChanges();
            editor.setText(mBefore);
            editor.setSelection(mSelStartBefore, mSelEndBefore);
            editor.registerTextChanges();
        }

        void redo(RTEditText editor) {
            editor.ignoreTextChanges();
            editor.setText(mAfter);
            editor.setSelection(mSelStartAfter, mSelEndAfter);
            editor.registerTextChanges();
        }
    }

    static class TextChangeOperation extends Operation {
        TextChangeOperation(Spannable before, Spannable after, int selStartBefore, int selEndBefore, int selStartAfter, int selEndAfter) {
            super(before, after, selStartBefore, selEndBefore, selStartAfter, selEndAfter);
        }
    }

    // ****************************************** execute/undo/redo/flush *******************************************

    /**
     * Call this when an operation is performed to add it to the undo stack.
     *
     * @param editor The rich text editor the operation was performed on
     * @param op     The Operation that was performed
     */
    synchronized void executed(RTEditText editor, Operation op) {
        Stack<Operation> undoStack = getUndoStack(editor);
        Stack<Operation> redoStack = getRedoStack(editor);

        // if operations are executed in a quick succession we "merge" them to have but one
        // -> saves memory and makes more sense from a user perspective (each key stroke an undo? -> no way)
        while (!undoStack.empty() && op.canMerge(undoStack.peek())) {
            Operation previousOp = undoStack.pop();
            op.merge(previousOp);
        }

        push(op, undoStack);
        redoStack.clear();
    }

    /**
     * Undo the last operation for a specific rich text editor
     *
     * @param editor Undo the last operation for this rich text editor
     */
    synchronized void undo(RTEditText editor) {
        Stack<Operation> undoStack = getUndoStack(editor);
        if (!undoStack.empty()) {
            Stack<Operation> redoStack = getRedoStack(editor);
            Operation op = undoStack.pop();
            push(op, redoStack);
            op.undo(editor);
            while (!undoStack.empty() && op.canMerge(undoStack.peek())) {
                op = undoStack.pop();
                push(op, redoStack);
                op.undo(editor);
            }
        }
    }

    /**
     * Re-do the last undone operation for a specific rich text editor
     *
     * @param editor Re-do an operation for this rich text editor
     */
    synchronized void redo(RTEditText editor) {
        Stack<Operation> redoStack = getRedoStack(editor);
        if (!redoStack.empty()) {
            Stack<Operation> undoStack = getUndoStack(editor);
            Operation op = redoStack.pop();
            push(op, undoStack);
            op.redo(editor);
            while (!redoStack.empty() && op.canMerge(redoStack.peek())) {
                op = redoStack.pop();
                push(op, undoStack);
                op.redo(editor);
            }
        }
    }

    /**
     * Flush all operations for a specific rich text editor (method unused at the moment)
     *
     * @param editor This rich text editor's operations will be flushed
     */
    synchronized void flushOperations(RTEditText editor) {
        Stack<Operation> undoStack = getUndoStack(editor);
        Stack<Operation> redoStack = getRedoStack(editor);
        undoStack.clear();
        redoStack.clear();
    }

    // ****************************************** Private Methods *******************************************

    private void push(Operation op, Stack<Operation> stack) {
        if (stack.size() >= MAX_NR_OF_OPERATIONS) {
            stack.remove(0);
        }
        stack.push(op);
    }

    private Stack<Operation> getUndoStack(RTEditText editor) {
        return getStack(mUndoStacks, editor);
    }

    private Stack<Operation> getRedoStack(RTEditText editor) {
        return getStack(mRedoStacks, editor);
    }

    private Stack<Operation> getStack(Map<Integer, Stack<Operation>> stacks, RTEditText editor) {
        Stack<Operation> stack = stacks.get(editor.getId());
        if (stack == null) {
            stack = new Stack<Operation>();
            stacks.put(editor.getId(), stack);
        }
        return stack;
    }

}
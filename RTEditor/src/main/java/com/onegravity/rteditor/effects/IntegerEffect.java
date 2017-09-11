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

package com.onegravity.rteditor.effects;

import android.util.Log;

import com.onegravity.rteditor.spans.RTSpan;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Base class for all Integer Effect classes like ForegroundColorEffect or AbsoluteSizeEffect.
 */
abstract class IntegerEffect<C extends RTSpan<Integer>> extends CharacterEffect<Integer, C> {
    private Class<? extends RTSpan<Integer>> mSpanClazz;

    protected IntegerEffect() {
        Type[] types = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
        mSpanClazz = (Class<? extends RTSpan<Integer>>) types[0];
    }

    @Override
    final protected RTSpan<Integer> newSpan(Integer value) {
        try {
            if (value != null) {
                Class[] paramTypes = {int.class};
                Constructor<? extends RTSpan<Integer>> constructor = mSpanClazz.getDeclaredConstructor(paramTypes);
                Integer[] params = {value};
                return constructor.newInstance(params);
            }
        } catch (IllegalAccessException e) {
            Log.e(getClass().getSimpleName(), "Exception instantiating " + mSpanClazz.getSimpleName(), e);
        } catch (NoSuchMethodException e) {
            Log.e(getClass().getSimpleName(), "Exception instantiating " + mSpanClazz.getSimpleName(), e);
        } catch (InstantiationException e) {
            Log.e(getClass().getSimpleName(), "Exception instantiating " + mSpanClazz.getSimpleName(), e);
        } catch (InvocationTargetException e) {
            Log.e(getClass().getSimpleName(), "Exception instantiating " + mSpanClazz.getSimpleName(), e);
        }

        return null;
    }

}

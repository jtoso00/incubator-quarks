/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
# Copyright IBM Corp. 2015, 2016 
*/
package quarks.topology.spi.graph;

import static quarks.window.Policies.alwaysInsert;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import quarks.function.BiFunction;
import quarks.function.Function;
import quarks.function.Functions;
import quarks.oplet.window.Aggregate;
import quarks.topology.TStream;
import quarks.window.Policies;
import quarks.window.Window;
import quarks.window.Windows;

public class TWindowImpl<T, K> extends AbstractTWindow<T, K> {
    private final int size;
    
    TWindowImpl(int size, TStream<T> feed, Function<T, K> keyFunction){
        super(feed, keyFunction);
        this.size = size;
    }

    @Override
    public <U> TStream<U> aggregate(BiFunction<List<T>,K, U> processor) { 
        processor = Functions.synchronizedBiFunction(processor);
        Window<T, K, LinkedList<T>> window = Windows.lastNProcessOnInsert(size, getKeyFunction());
        Aggregate<T,U,K> op = new Aggregate<T,U,K>(window, processor);
        return feeder().pipe(op); 
    }

    @Override
    public <U> TStream<U> batch(BiFunction<List<T>, K, U> batcher) {
        batcher = Functions.synchronizedBiFunction(batcher);
        Window<T, K, List<T>> window =
                Windows.window(
                        alwaysInsert(),
                        Policies.doNothing(),
                        Policies.evictAll(),
                        Policies.processWhenFullAndEvict(size),
                        getKeyFunction(),
                        () -> new ArrayList<T>(size));
        
        Aggregate<T,U,K> op = new Aggregate<T,U,K>(window, batcher);
        return feeder().pipe(op); 
    }
}

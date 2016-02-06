package com.aol.cyclops.control;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import com.aol.cyclops.lambda.applicative.Applicativable;
import com.aol.cyclops.lambda.monads.ConvertableFunctor;
import com.aol.cyclops.lambda.monads.FlatMap;
import com.aol.cyclops.lambda.monads.Functor;
import com.aol.cyclops.lambda.monads.ToAnyM;
import com.aol.cyclops.monad.AnyM;
import com.aol.cyclops.sequence.SequenceM;
import com.aol.cyclops.value.Value;

import lombok.AllArgsConstructor;
@AllArgsConstructor
public class FutureFunctor<T> implements ConvertableFunctor<T>,
											Applicativable<T>, 
											Value<T>, 
											FlatMap<T>,
											ToAnyM<T>{

	public static <T> FutureFunctor<T> of(CompletableFuture<T> f){
		return new FutureFunctor<>(f);
	}

	//public static 
	private final CompletableFuture<T> future;

	@Override
	public <R> Functor<R> map(Function<? super T, ? extends R> fn) {
		return new FutureFunctor<R>(future.thenApply(fn));
	}

	@Override
	public T get() {
		return future.join();
	}

	@Override
	public Iterator<T> iterator() {
		return toStream().iterator();
	}
	/* (non-Javadoc)
	 * @see com.aol.cyclops.lambda.monads.Unit#unit(java.lang.Object)
	 */
	@Override
	public <T> FutureFunctor<T> unit(T unit) {
		return new FutureFunctor<T>(CompletableFuture.completedFuture(unit));
	}

	@Override
	public SequenceM<T> stream() {
		return SequenceM.generate(()->get()).limit(1);
	}

	@Override
	public <R> FutureFunctor<R> flatten() {
		return FutureFunctor.of(AnyM.fromCompletableFuture(future).flatten().unwrap());
	}
	public <R> FutureFunctor<R> flatMap(Function<? super T, ? extends CompletionStage<? extends R>> mapper){
		return FutureFunctor.<R>of(future.<R>thenCompose(t->(CompletionStage)mapper.apply(t)));
		
	}
}

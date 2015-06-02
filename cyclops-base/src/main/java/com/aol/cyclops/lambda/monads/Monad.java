package com.aol.cyclops.lambda.monads;

import static com.aol.cyclops.lambda.api.AsGenericMonad.asMonad;
import static com.aol.cyclops.lambda.api.AsGenericMonad.monad;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.experimental.Delegate;
import lombok.Value;
import lombok.val;

import com.aol.cyclops.lambda.api.AsGenericMonad;
import com.aol.cyclops.lambda.api.AsStreamable;
import com.aol.cyclops.lambda.api.Monoid;
import com.aol.cyclops.lambda.api.Streamable;
import com.aol.cyclops.streams.StreamUtils;
import com.nurkiewicz.lazyseq.LazySeq;



/**
 * An interoperability Trait that encapsulates java Monad implementations.
 * 
 * A generalised view into Any Monad (that implements flatMap or bind and accepts any function definition
 * with an arity of 1).
 * 
 * NB the intended use case is to wrap already existant Monad-like objects from diverse sources, to improve
 * interoperability - it's not intended for use as an interface to be implemented on a Monad.
 * 
 * @author johnmcclean
 *
 * @param <T>
 * @param <MONAD>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public interface Monad<MONAD,T> extends Functor<T>, Filterable<T>, Streamable<T>, AsGenericMonad{
	
	
	
	public <MONAD,T> Monad<MONAD,T> withMonad(Object invoke);
	public Object getMonad();
	
	default <T> Monad<MONAD,T> withFunctor(T functor){
		return withMonad(functor);
	}
	default Object getFunctor(){
		return getMonad();
	}
	default Filterable<T> withFilterable(Filterable filter){
		return withMonad(filter);
	}
	default Object getFilterable(){
		return getMonad();
	}
	/* (non-Javadoc)
	 * @see com.aol.cyclops.lambda.monads.Filterable#filter(java.util.function.Predicate)
	 */
	default   Monad<MONAD,T>  filter(Predicate<T> fn){
		return (Monad)Filterable.super.filter(fn);
	}
	/* (non-Javadoc)
	 * @see com.aol.cyclops.lambda.monads.Functor#map(java.util.function.Function)
	 */
	default  <R> Monad<MONAD,R> map(Function<T,R> fn){
		return (Monad)Functor.super.map(fn);
	}
	/* (non-Javadoc)
	 * @see com.aol.cyclops.lambda.monads.Functor#peek(java.util.function.Consumer)
	 */
	default   Monad<MONAD,T>  peek(Consumer<T> c) {
		return (Monad)Functor.super.peek(c);
	}
	/**
	 * True if predicate matches all elements when Monad converted to a Stream
	 * 
	 * @param c Predicate to check if all match
	 */
	default  void  allMatch(Predicate<T> c) {
		stream().allMatch(c);
	}
	/**
	 * True if a single element matches when Monad converted to a Stream
	 * 
	 * @param c Predicate to check if any match
	 */
	default  void  anyMatch(Predicate<T> c) {
		stream().anyMatch(c);
	}
	/**
	 * @return First matching element in sequential order
	 * 
	 * (deterministic)
	 * 
	 */
	default  Optional<T>  findFirst() {
		return stream().findFirst();
	}
	/**
	 * @return first matching element,  but order is not guaranteed
	 * 
	 * (non-deterministic) 
	 */
	default  Optional<T>  findAny() {
		return stream().findAny();
	}
	
	/**
	 * Perform a looser typed flatMap / bind operation
	 * The return type can be another type other than the host type
	 * 
	 * @param fn flatMap function
	 * @return flatMapped monad
	 */
	default <R> Monad<MONAD,T> bind(Function<T,R> fn){
		return withMonad((MONAD)new ComprehenderSelector().selectComprehender(
				getMonad())
				.executeflatMap(getMonad(), fn));
	
	}
	/**
	 * Perform a bind operation (@see #bind) but also lift the return value into a Monad using configured
	 * MonadicConverters
	 * 
	 * @param fn flatMap function
	 * @return flatMapped monad
	 */
	default <MONAD1,R> Monad<MONAD1,R> liftAndBind(Function<T,?> fn){
		return withMonad((MONAD)new ComprehenderSelector().selectComprehender(
				getMonad())
				.liftAndFlatMap(getMonad(), fn));
	
	}
	
	/**
	 * join / flatten one level of a nested hierarchy
	 * 
	 * @return Flattened / joined one level
	 */
	default <T1> Monad<T,T1> flatten(){
		return (Monad)this.flatMap( t->   (MONAD)t );
		
	}
	/**
	 * Attempt to map this Monad to the same type as the supplied Monoid (using mapToType on the monoid interface)
	 * Then use Monoid to reduce values
	 * 
	 * @param reducer Monoid to reduce values
	 * @return Reduce result
	 */
	default  <R> R mapReduce(Monoid<R> reducer){
		return reducer.mapReduce(stream());
	}
	/**
	 *  Attempt to map this Monad to the same type as the supplied Monoid, using supplied function
	 *  Then use Monoid to reduce values
	 *  
	 * @param mapper Function to map Monad type
	 * @param reducer Monoid to reduce values
	 * @return Reduce result
	 */
	default  <R> R mapReduce(Function<T,R> mapper, Monoid<R> reducer){
		return reducer.reduce(stream().map(mapper));
	}
	
	/**
	 * Mutable reduction / collection over this Monad converted to a Stream
	 * 
	 * @param collector Collection operation definition
	 * @return Collected result
	 */
	default <R, A> R collect(Collector<T,A,R> collector){
		return stream().collect(collector);
	}
	
	/**
	 * NB if this Monad is an Optional [Arrays.asList(1,2,3)]  reduce will operate on the Optional as if the list was one value
	 * To reduce over the values on the list, called streamedMonad() first. I.e. streamedMonad().reduce(reducer)
	 * 
	 * 
	 * @param reducer Use supplied Monoid to reduce values
	 * @return reduced values
	 */
	default  T reduce(Monoid<T> reducer){
		return reducer.reduce(stream());
	}
	
	/**
     * Reduce with multiple reducers in parallel
	 * NB if this Monad is an Optional [Arrays.asList(1,2,3)]  reduce will operate on the Optional as if the list was one value
	 * To reduce over the values on the list, called streamedMonad() first. I.e. streamedMonad().reduce(reducer)
	 * 
	 * @param reducers
	 * @return
	 */
	default List<T> reduce(Stream<Monoid<T>> reducers){
		return StreamUtils.reduce(stream(), reducers);
	}
	/**
     * Reduce with multiple reducers in parallel
	 * NB if this Monad is an Optional [Arrays.asList(1,2,3)]  reduce will operate on the Optional as if the list was one value
	 * To reduce over the values on the list, called streamedMonad() first. I.e. streamedMonad().reduce(reducer)
	 * 
	 * @param reducers
	 * @return
	 */
	default List<T> reduce(Iterable<Monoid<T>> reducers){
		return StreamUtils.reduce(stream(), reducers);
	}
	
	/**
	 * 
	 * 
	 * @param reducer Use supplied Monoid to reduce values starting via foldLeft
	 * @return Reduced result
	 */
	default T foldLeft(Monoid<T> reducer){
		return reduce(reducer);
	}
	/**
	 *  Attempt to map this Monad to the same type as the supplied Monoid (using mapToType on the monoid interface)
	 * Then use Monoid to reduce values
	 * 
	 * @param reducer Monoid to reduce values
	 * @return Reduce result
	 */
	default <T> T foldLeftMapToType(Monoid<T> reducer){
		return reducer.mapReduce(stream());
	}
	/**
	 * 
	 * 
	 * @param reducer Use supplied Monoid to reduce values starting via foldRight
	 * @return Reduced result
	 */
	default T foldRight(Monoid<T> reducer){
		return reducer.reduce(StreamUtils.reverse(stream()));
	}
	/**
	 *  Attempt to map this Monad to the same type as the supplied Monoid (using mapToType on the monoid interface)
	 * Then use Monoid to reduce values
	 * 
	 * @param reducer Monoid to reduce values
	 * @return Reduce result
	 */
	default <T> T foldRightMapToType(Monoid<T> reducer){
		return reducer.mapReduce(StreamUtils.reverse(stream()));
	}
	/**
	 * @return Underlying monad converted to a Streamable instance
	 */
	default Streamable<T> toStreamable(){
		return  AsStreamable.asStreamable(stream());
	}
	/**
	 * @return This monad converted to a set
	 */
	default Set<T> toSet(){
		return (Set)stream().collect(Collectors.toSet());
	}
	/**
	 * @return this monad converted to a list
	 */
	default List<T> toList(){
		return (List)stream().collect(Collectors.toList());
	}
	/**
	 * @return  calls to stream() but more flexible on type for inferencing purposes.
	 */
	default <T> Stream<T> toStream(){
		return (Stream)stream();
	}
	/**
	 * Unwrap this Monad into a Stream.
	 * If the underlying monad is a Stream it is returned
	 * Otherwise we flatMap the underlying monad to a Stream type
	 */
	default Stream<T> stream(){
		if(unwrap() instanceof Stream)
			return (Stream)unwrap();
		Stream stream = Stream.of(1);
		return (Stream)withMonad((Stream)new ComprehenderSelector().selectComprehender(
				stream).executeflatMap(stream, i-> getMonad())).flatMap(Function.identity()).unwrap();
		
	}
	/**
	 * @return This monad coverted to an Optional
	 * 
	 * Streams will be converted into Optional<List<T>>
	 * 
	 */
	default <T> Optional<T> toOptional(){
		Optional stream = Optional.of(1);
		return this.<Optional,T>withMonad((Optional)new ComprehenderSelector().selectComprehender(
				stream).executeflatMap(stream, i-> getMonad())).unwrap();
		
	}
	
	/**
	 * Convert to a Stream with the values repeated specified times
	 * 
	 * @param times Times values should be repeated within a Stream
	 * @return Stream with values repeated
	 */
	default Monad<Stream<T>,T> cycle(int times){
		
		return monad(StreamUtils.cycle(times,AsStreamable.asStreamable(stream())));
		
	}
	
	/**
	 * Convert to a Stream with the result of a reduction operation repeated specified times
	 * 
	 * {@code 
	  		List<Integer> list = AsGenericMonad,asMonad(Stream.of(1,2,2))
											.cycle(Reducers.toCountInt(),3)
											.collect(Collectors.toList());
		//is asList(3,3,3);
	  }
	 * 
	 * @param m Monoid to be used in reduction
	 * @param times Number of times value should be repeated
	 * @return Stream with reduced values repeated
	 */
	default Monad<Stream<T>,T> cycle(Monoid<T> m,int times){
		return monad(StreamUtils.cycle(times,AsStreamable.asStreamable(m.reduce(stream()))));
	}
	
	
	/**
	 * 
	 * Convert to a Stream, repeating the resulting structure specified times and
	 * lifting all values to the specified Monad type
	 * 
	 * {@code
	 * 
	 *  List<Optional<Integer>> list  = monad(Stream.of(1,2))
											.cycle(Optional.class,2)
											.toList();
											
	    //is asList(Optional.of(1),Optional.of(2),Optional.of(1),Optional.of(2)	));
	
	 * 
	 * }
	 * 
	 * 
	 * 
	 * @param monad
	 * @param times
	 * @return
	 */
	default <R> Monad<Stream<R>,R> cycle(Class<R> monad,int times){
		return (Monad)cycle(times).map(r -> new ComprehenderSelector().selectComprehender(monad).of(r));	
	}

	/**
	 * Repeat in a Stream while specified predicate holds
	 * 
	 * @param predicate repeat while true
	 * @return Repeating Stream
	 */
	default  Monad<Stream<T>,T> cycleWhile(Predicate<T> predicate){
		return monad(LazySeq.of(StreamUtils.cycle(stream()).iterator()).takeWhile(predicate).stream());
	}
	/**
	 * Repeat in a Stream until specified predicate holds
	 * 
	 * @param predicate repeat while true
	 * @return Repeating Stream
	 */
	default  Monad<Stream<T>,T> cycleUntil(Predicate<T> predicate){
		return monad(LazySeq.of(StreamUtils.cycle(stream()).iterator()).takeWhile(predicate.negate()).stream());
	}
	/**
	 * Generic zip function. E.g. Zipping a Stream and an Optional
	 * 
	 * {@code
	 * Stream<List<Integer>> zipped = asMonad(Stream.of(1,2,3)).zip(asMonad(Optional.of(2)), 
													(a,b) -> Arrays.asList(a,b));
	 * // [[1,2]]
	 * }
	 * 
	 * @param second Monad to zip with
	 * @param zipper Zipping function
	 * @return Stream zipping two Monads
	 */
	default <MONAD2,S,R> Monad<Stream<R>,R> zip(Monad<MONAD2,? extends S> second, BiFunction<? super T, ? super S, ? extends R> zipper){
		return monad((Stream)LazySeq.of(stream().iterator()).zip(LazySeq.of(second.stream().iterator()), zipper).stream());
	}
	
	/**
	 * Zip this Monad with a Stream
	 * 
	 * {@code 
	 * Stream<List<Integer>> zipped = asMonad(Stream.of(1,2,3)).zip(Stream.of(2,3,4), 
													(a,b) -> Arrays.asList(a,b));
													
		//[[1,2][2,3][3,4]]											
	 * }
	 * 
	 * @param second Stream to zip with
	 * @param zipper  Zip funciton
	 * @return This monad zipped with a Stream
	 */
	default <S,R> Monad<Stream<R>,R> zip(Stream<? extends S> second, BiFunction<? super T, ? super S, ? extends R> zipper){
		return monad((Stream)LazySeq.of(stream().iterator()).zip(LazySeq.of(second.iterator()), zipper).stream());
	}
	/**
	 * Create a sliding view over this monad
	 * 
	 * @param windowSize Size of sliding window
	 * @return Stream with sliding view over monad
	 */
	default Monad<Stream<List<T>>,List<T>> sliding(int windowSize){
		return monad((Stream)LazySeq.of(stream().iterator()).sliding(windowSize).stream());
	}
	
	/**
	 * Group elements in a Monad into a Stream
	 * 
	 * {@code
	 * 
	 * List<List<Integer>> list = monad(Stream.of(1,2,3,4,5,6))
	 * 									.grouped(3)
	 * 									.collect(Collectors.toList());
		
		
		assertThat(list.get(0),hasItems(1,2,3));
		assertThat(list.get(1),hasItems(4,5,6));
		
		}
	 * 
	 * @param groupSize Size of each Group
	 * @return Stream with elements grouped by size
	 */
	default Monad<Stream<List<T>>,List<T>> grouped(int groupSize){
		return monad(LazySeq.of(stream().iterator()).grouped(groupSize).stream());
	}
	/**
	 * 
	 * {@code 
	 * assertTrue(monad(Stream.of(1,2,3,4)).startsWith(Arrays.asList(1,2,3)));
	 * }
	 * 
	 * @param iterable
	 * @return True if Monad starts with Iterable sequence of data
	 */
	default boolean startsWith(Iterable<T> iterable){
		return LazySeq.of(stream().iterator()).startsWith(iterable);
		
	}
	/**
	 * 	{@code assertTrue(monad(Stream.of(1,2,3,4)).startsWith(Arrays.asList(1,2,3).iterator())) }

	 * @param iterator
	 * @return True if Monad starts with Iterators sequence of data
	 */
	default boolean startsWith(Iterator<T> iterator){
		return LazySeq.of(stream().iterator()).startsWith(iterator);
		
	}
	
	
	/*
	 * Return the distinct Stream of elements
	 * 
	 * {@code
	 * 	List<Integer> list = monad(Optional.of(Arrays.asList(1,2,2,2,5,6)))
											.<Stream<Integer>,Integer>streamedMonad()
											.distinct()
											.collect(Collectors.toList());
		}
	 */
	default Monad<Stream<T>,T> distinct(){
		return monad(LazySeq.of(stream().iterator()).distinct().stream());
	}
	default Monad<Stream<T>,T> scanLeft(Monoid<T> monoid){
		return AsGenericMonad.monad(LazySeq.of(stream().iterator()).scan(monoid.zero(), monoid.reducer()).stream());
	}
	
	default Monad<Stream<T>,T> sorted(){
		return monad(stream().sorted());
	}
	default Monad<Stream<T>,T> sorted(Comparator<T > c){
		return monad(stream().sorted(c));   
	}
	default Monad<Stream<T>,T> skip(int num){
		return monad(stream().skip(num));
	}
	default Monad<Stream<T>,T> skipWhile(Predicate<T> p){
		return monad(LazySeq.of(stream().iterator()).dropWhile(p).stream());
	}
	default Monad<Stream<T>,T> skipUntil(Predicate<T> p){
		return monad(LazySeq.of(stream().iterator()).dropWhile(p.negate()).stream());
	}
	default Monad<Stream<T>,T> limit(int num){
		return monad(stream().limit(num));
	}
	default Monad<Stream<T>,T> limitWhile(Predicate<T> p){
		return monad(LazySeq.of(stream().iterator()).takeWhile(p).stream());
	}
	default Monad<Stream<T>,T> limiUntil(Predicate<T> p){
		return monad(LazySeq.of(stream().iterator()).takeWhile(p.negate()).stream());
	}
	
	
	/**
	 * Convert a list of Monads to a Monad with a List
	 * 
	 * {@code
	 * List<CompletableFuture<Integer>> futures;

        
        CompletableFuture<List<Integer>> futureList = Monad.sequence(CompletableFuture.class, futures);

	  
	  }
	 * 
	 * @param c The type of Monad to convert
	 * @param seq List of monads to convert
	 * @return Monad with a List
	 */ 	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <MONAD,MONAD_LIST> MONAD_LIST sequenceNative(Class c,List<MONAD> seq){
		return (MONAD_LIST)AsGenericMonad.asMonad(new ComprehenderSelector().selectComprehender(c).of(1))
								.flatMap(in-> 
											AsGenericMonad.asMonad(seq.stream()).flatMap(m-> m).unwrap()
											).unwrap();
	}
	/**
	 * Convert a list of Monads to a Monad with a List applying the supplied function in the process
	 * 
	 * {@code 
	 *    List<CompletableFuture<Integer>> futures;

        
        CompletableFuture<List<String>> futureList = Monad.traverse(CompletableFuture.class, futures, (Integer i) -> "hello" +i);
        }

	 * 
	 * @param c Monad type to traverse
	 * @param seq List of Monads
	 * @param fn Function to apply 
	 * @return Monad with a list
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <MONAD,MONAD_LIST,R> MONAD_LIST traverseNative(Class c,List<MONAD> seq, Function<?,R> fn){
		return (MONAD_LIST)AsGenericMonad.asMonad(new ComprehenderSelector().selectComprehender(c).of(1))
								.flatMap(in-> 
											AsGenericMonad.asMonad(seq.stream()).flatMap(m-> m).flatMap((Function)fn).unwrap()
											).unwrap();
	}
	
	/**
	 * Convert a list of Monads to a Monad with a List applying the supplied function in the process
	 * 
	 * {@code 
	 *    List<CompletableFuture<Integer>> futures;

        
        Simplex<List<String>> futureList = Monad.traverse(CompletableFuture.class, futures, (Integer i) -> "hello" +i).simplex();
        }

	 * 
	 * @param c Monad type to traverse
	 * @param seq List of Monads
	 * @param fn Function to apply 
	 * @return Monad with a list
	 */
	public static <MONAD,R> Monad<MONAD,List<R>> traverse(Class c,List<?> seq, Function<?,R> fn){
		return (Monad)asMonad(new ComprehenderSelector().selectComprehender(c).of(1))
								.flatMap(in-> asMonad(seq.stream()).flatMap(m-> m).flatMap((Function)fn).unwrap()
									);
	}

	
	/**
	 * Convert a list of Monads to a Monad with a List
	 * 
	 * {@code
	 * List<CompletableFuture<Integer>> futures;

        
        Simplex<List<Integer>> futureList = Monad.sequence(CompletableFuture.class, futures).simplex();

	  
	  }
	 * 
	 * @param c The type of Monad to convert
	 * @param seq List of monads to convert
	 * @return Monad with a List
	 */ 
	public static <MONAD,T>  Monad<MONAD,T> sequence(Class c, List<?> seq){
		return (Monad)AsGenericMonad.asMonad(new ComprehenderSelector().selectComprehender(c).of(1))
				.flatMap(in-> asMonad(seq.stream()).flatMap(m-> m).unwrap()
							);
	}
	default <R> Monad<MONAD,R> aggregate(Monad<?,?> next){
		Stream concat = StreamUtils.concat(stream(),next.stream() );
		
		return (Monad)withMonad(new ComprehenderSelector().selectComprehender(
				unwrap()).of(monad(concat)
						.flatMap(Function.identity())
						.toList()))
						.bind(Function.identity() );
	}
	
	/**
	 * flatMap operation
	 * 
	 * @param fn
	 * @return
	 */
	default <R extends MONAD,NT> Monad<R,NT> flatMap(Function<T,R> fn) {
		return (Monad)bind(fn);
	}
	default   MONAD unwrap(){
		return (MONAD)getMonad();
	}
	
	default <R,NT> Monad<R,NT> parallel(){
		return streamedMonad().parallel();
	}
	
	/**
	 * Transform the contents of a Monad into a Monad wrapping a Stream e.g.
	 * Turn an Optional<List<Integer>> into Stream<Integer>
	 * 
	 * {@code
	 * List<List<Integer>> list = monad(Optional.of(Arrays.asList(1,2,3,4,5,6)))
											.<Stream<Integer>,Integer>streamedMonad()
											.grouped(3)
											.collect(Collectors.toList());
		
		
		assertThat(list.get(0),hasItems(1,2,3));
		assertThat(list.get(1),hasItems(4,5,6));
	 * 
	 * }
	 * 
	 * 
	 * @return A Monad that wraps a Stream
	 */
	default <R,NT> Monad<R,NT> streamedMonad(){
		Stream stream = Stream.of(1);
		 Monad r = this.<Stream,T>withMonad((Stream)new ComprehenderSelector().selectComprehender(
				stream).executeflatMap(stream, i-> getMonad()));
		 return r.flatMap(e->e);
	}
	
	static interface FlatMap<T>{
		public  <R1, NT> Monad<R1, NT> flatMap(Function<T, R1> fn);
	}
	/**
	 * @return Simplex view on wrapped Monad, with a single typed parameter - which is the datatype
	 * ultimately being handled by the Monad.
	 * 
	 * E.g.
	 * {@ code 
	 * Monad<Stream<String>,String> becomes
	 * Simplex<String>
	 * }
	 * To get back to Stream<String> use
	 * 
	 * {@code
	 *   
	 * simplex.<Stream<String>>.monad();  
	 * }
	 * 
	 */
	default <X> Simplex<X> simplex(){
		return new Simplex<X>(){
			@Delegate(excludes=FlatMap.class)
			Monad<Object,X> m = (Monad)Monad.this;

			public  <R1, NT> Monad<R1, NT> flatMap(Function<X, R1> fn) {
				return m.flatMap(fn);
			}
			
			public <R> R monad(){
				return (R)unwrap();
			}
			
		};
	}
	

}

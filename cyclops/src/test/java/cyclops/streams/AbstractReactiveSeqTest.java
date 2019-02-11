package cyclops.streams;

import cyclops.data.Vector;
import cyclops.reactive.ReactiveSeq;
import cyclops.reactive.Spouts;
import org.junit.Test;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public abstract class AbstractReactiveSeqTest {

    public abstract ReactiveSeq<Integer> of(Integer... values);
    public abstract ReactiveSeq<Integer> empty();
    /** recoverWith tests **/
    @Test
    public void recoverWithList(){

        List<Integer> result = of(1, 2, 3).<Integer>map(i -> {
                throw new RuntimeException();
            })
                .recoverWith(Spouts.of(100,200,300))
                .toList();




        assertThat(result,equalTo(Arrays.asList(100,200,300)));
    }
    @Test
    public void recoverWithIterator(){

        Iterator<Integer> it = of(1, 2, 3).<Integer>map(i -> {
            throw new RuntimeException();
        })
            .recoverWith(Spouts.of(100,200,300))
            .iterator();

        List<Integer> result = new ArrayList<>();
        while(it.hasNext()){
            result.add(it.next());
        }



        assertThat(result,equalTo(Arrays.asList(100,200,300)));
    }
    @Test
    public void recoverWithMiddleList(){

        List<Integer> result = of(1, 2, 3).<Integer>map(i -> {
            if(i==2)
                throw new RuntimeException();
            return i;
        })
            .recoverWith(Spouts.of(100,200,300))
            .toList();




        assertThat(result,equalTo(Arrays.asList(1,100,200,300)));
    }
    @Test
    public void recoverWithMiddleIterator(){

        Iterator<Integer> it = of(1, 2, 3).<Integer>map(i -> {
            if(i==2)
                throw new RuntimeException();
            return i;
        })
            .recoverWith(Spouts.of(100,200,300))
            .iterator();

        List<Integer> result = new ArrayList<>();
        while(it.hasNext()){
            result.add(it.next());
        }



        assertThat(result,equalTo(Arrays.asList(1,100,200,300)));
    }

    @Test
    public void recoverWithNoErrorList(){

        List<Integer> result = of(1, 2, 3).<Integer>map(i -> i)
            .recoverWith(of(100,200,300))
            .toList();




        assertThat(result,equalTo(Arrays.asList(1,2,3)));
    }
    @Test
    public void recoverWithNoErrorIterator(){

        Iterator<Integer> it =  of(1, 2, 3).<Integer>map(i -> i)
            .recoverWith(of(100,200,300))
            .iterator();

        List<Integer> result = new ArrayList<>();
        while(it.hasNext()){
            result.add(it.next());
        }


        assertThat(result,equalTo(Arrays.asList(1,2,3)));
    }

    @Test
    public void recoverWith(){
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean data = new AtomicBoolean(false);
        AtomicReference<Vector<Integer>> result = new AtomicReference<>(Vector.empty());
        AtomicBoolean complete = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<Throwable>(null);

        of(1, 2, 3).<Integer>map(i -> {
            throw new RuntimeException();
        })
            .recoverWith(Spouts.of(100,200,300))
            .forEach(n -> {
                data.set(true);
                result.updateAndGet(v->v.plus(n));
            }, e -> {
                error.set(e);
            }, () -> {
                complete.set(true);
            });

        assertThat(data.get(), equalTo(true));
        assertThat(complete.get(), equalTo(true));
        assertThat(error.get(), equalTo(null));
        assertThat(result.get(),equalTo(Vector.of(100,200,300)));



    }
    @Test
    public void recoverWithIncremental(){
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean data = new AtomicBoolean(false);
        AtomicReference<Vector<Integer>> result = new AtomicReference<>(Vector.empty());
        AtomicBoolean complete = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<Throwable>(null);

        Subscription sub = of(1, 2, 3).<Integer>map(i -> {
            throw new RuntimeException();
        })
            .recoverWith(Spouts.of(100,200,300))
            .forEach(0,n -> {
                data.set(true);
                result.updateAndGet(v->v.plus(n));
            }, e -> {
                error.set(e);
            }, () -> {
                complete.set(true);
            });

        assertThat(data.get(), equalTo(false));
        assertThat(complete.get(), equalTo(false));
        assertThat(error.get(), equalTo(null));
        assertThat(result.get(),equalTo(Vector.empty()));

        sub.request(1l);
        assertThat(data.get(), equalTo(true));
        assertThat(complete.get(), equalTo(false));
        assertThat(error.get(), equalTo(null));
        assertThat(result.get(),equalTo(Vector.of(100)));

        sub.request(1000l);
        assertThat(data.get(), equalTo(true));
        assertThat(complete.get(), equalTo(true));
        assertThat(error.get(), equalTo(null));
        assertThat(result.get(),equalTo(Vector.of(100,200,300)));


    }

    @Test
    public void recoverWithEmptyList(){

        List<Integer> result = of().<Integer>map(i -> {
            throw new RuntimeException();
        })
            .recoverWith(Spouts.of(100,200,300))
            .toList();




        assertThat(result,equalTo(Arrays.asList()));
    }
    /** onError tests **/

    @Test
    public void onErrorList(){
        AtomicInteger count = new AtomicInteger(0);

        try {
            of(1, 2, 3).map(i -> {
                throw new RuntimeException();
            })
                .onError(e -> count.incrementAndGet())
                .toList();
            fail("exception expected");
        }catch(Exception e){

        }


        assertThat(count.get(),equalTo(1));

    }
    @Test
    public void onErrorIterator(){
        AtomicInteger count = new AtomicInteger(0);

        try {
            Iterator<Integer> it = of(1, 2, 3).<Integer>map(i -> {
                throw new RuntimeException();
            })
                .onError(e -> count.incrementAndGet())
                .iterator();
            while(it.hasNext()){
                System.out.println(it.next());
            }
            fail("exception expected");
        }catch(Exception e){

        }


        assertThat(count.get(),equalTo(1));

    }
    @Test
    public void onError(){
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean data = new AtomicBoolean(false);
        AtomicReference<Vector<Integer>> result = new AtomicReference<>(Vector.empty());
        AtomicBoolean complete = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<Throwable>(null);



            of(1, 2, 3).<Integer>map(i -> {
                throw new RuntimeException();
            })
                .onError(e -> count.incrementAndGet())
                .forEach(n -> {
                    data.set(true);
                    result.updateAndGet(v->v.plus(n));
                }, e -> {
                    error.set(e);
                }, () -> {
                    complete.set(true);
                });

            assertThat(data.get(), equalTo(false));
            assertThat(complete.get(), equalTo(true));
            assertThat(error.get(), instanceOf(RuntimeException.class));
            assertThat(result.get(),equalTo(Vector.empty()));




        assertThat(count.get(),equalTo(3));

    }
    @Test
    public void onErrorIncremental(){
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean data = new AtomicBoolean(false);
        AtomicReference<Vector<Integer>> result = new AtomicReference<>(Vector.empty());
        AtomicBoolean complete = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<Throwable>(null);



        Subscription sub =of(1, 2, 3).<Integer>map(i -> {
            throw new RuntimeException();
        })
            .onError(e -> count.incrementAndGet())
            .forEach(0,n -> {
                data.set(true);
                result.updateAndGet(v->v.plus(n));
            }, e -> {
                error.set(e);
            }, () -> {
                complete.set(true);
            });

        assertThat(data.get(), equalTo(false));
        assertThat(complete.get(), equalTo(false));
        assertThat(error.get(), equalTo(null));
        assertThat(result.get(),equalTo(Vector.empty()));

        sub.request(1l);
        assertThat(data.get(), equalTo(false));
        assertThat(complete.get(), equalTo(false));
        assertThat(error.get(), instanceOf(RuntimeException.class));
        assertThat(result.get(),equalTo(Vector.empty()));

        sub.request(100l);
        assertThat(data.get(), equalTo(false));
        assertThat(complete.get(), equalTo(true));
        assertThat(error.get(), instanceOf(RuntimeException.class));
        assertThat(result.get(),equalTo(Vector.empty()));



        assertThat(count.get(),equalTo(3));

    }
    @Test
    public void onErrorEmptyList(){
        AtomicInteger count = new AtomicInteger(0);

        try {
            empty().map(i -> {
                throw new RuntimeException();
            })
                .onError(e -> count.incrementAndGet())
                .toList();

        }catch(Exception e){

        }


        assertThat(count.get(),equalTo(0));

    }
    @Test
    public void onErrorEmptyIterator(){
        AtomicInteger count = new AtomicInteger(0);

        try {
            Iterator<Integer> it = empty().<Integer>map(i -> {
                throw new RuntimeException();
            })
                .onError(e -> count.incrementAndGet())
                .iterator();
            while(it.hasNext()){
                System.out.println(it.next());
            }

        }catch(Exception e){

        }


        assertThat(count.get(),equalTo(0));

    }
    @Test
    public void onErrorEmpty(){
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean data = new AtomicBoolean(false);
        AtomicReference<Vector<Integer>> result = new AtomicReference<>(Vector.empty());
        AtomicBoolean complete = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<Throwable>(null);



        empty().<Integer>map(i -> {
            throw new RuntimeException();
        })
            .onError(e -> count.incrementAndGet())
            .forEach(n -> {
                data.set(true);
                result.updateAndGet(v->v.plus(n));
            }, e -> {
                error.set(e);
            }, () -> {
                complete.set(true);
            });

        assertThat(data.get(), equalTo(false));
        assertThat(complete.get(), equalTo(true));
        assertThat(error.get(), equalTo(null));
        assertThat(result.get(),equalTo(Vector.empty()));




        assertThat(count.get(),equalTo(0));

    }

    @Test
    public void onErrorEmptyIncremental(){
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean data = new AtomicBoolean(false);
        AtomicReference<Vector<Integer>> result = new AtomicReference<>(Vector.empty());
        AtomicBoolean complete = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<Throwable>(null);



        Subscription sub = empty().<Integer>map(i -> {
            throw new RuntimeException();
        })
            .onError(e -> count.incrementAndGet())
            .forEach(0,n -> {
                data.set(true);
                result.updateAndGet(v->v.plus(n));
            }, e -> {
                error.set(e);
            }, () -> {
                complete.set(true);
            });

        assertThat(data.get(), equalTo(false));
        assertThat(complete.get(), equalTo(false));
        assertThat(error.get(), equalTo(null));
        assertThat(result.get(),equalTo(Vector.empty()));

        sub.request(1l);
        assertThat(data.get(), equalTo(false));
        assertThat(complete.get(), equalTo(true));
        assertThat(error.get(), equalTo(null));
        assertThat(result.get(),equalTo(Vector.empty()));



        assertThat(count.get(),equalTo(0));

    }


}

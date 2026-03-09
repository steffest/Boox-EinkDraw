# AAR API Reference (Trimmed)

Focused subset of `docs/aar-api-reference.md` for APIs currently used by this app.

## Filter Criteria

- `com.onyx.android.sdk.pen.*`
- `com.onyx.android.sdk.pen.data.*`
- `com.onyx.android.sdk.api.device.epd.*`
- `com.onyx.android.sdk.rx.*`
- `com.onyx.android.sdk.data.note.TouchPoint`

## onyxsdk-base-1.8.4.aar

- Included classes/interfaces: 64

#### `com.onyx.android.sdk.data.note.TouchPoint`

- `public class com.onyx.android.sdk.data.note.TouchPoint extends com.onyx.android.sdk.base.data.TouchPoint`
- `public static final int OBJECT_BYTE_COUNT;`
- `public com.onyx.android.sdk.data.note.TouchPoint();`
- `public com.onyx.android.sdk.data.note.TouchPoint(float, float);`
- `public com.onyx.android.sdk.data.note.TouchPoint(float, float, float, float, long);`
- `public com.onyx.android.sdk.data.note.TouchPoint(float, float, float, float, int, int, long);`
- `public com.onyx.android.sdk.data.note.TouchPoint(android.view.MotionEvent);`
- `public com.onyx.android.sdk.data.note.TouchPoint(com.onyx.android.sdk.data.note.TouchPoint);`
- `public static com.onyx.android.sdk.data.note.TouchPoint create(android.view.MotionEvent);`
- `public static com.onyx.android.sdk.data.note.TouchPoint fromHistorical(android.view.MotionEvent, int);`
- `public static java.util.List<com.onyx.android.sdk.data.note.TouchPoint> renderPointArray(android.graphics.Matrix, java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public static float getPointAngle(com.onyx.android.sdk.data.note.TouchPoint, com.onyx.android.sdk.data.note.TouchPoint);`
- `public static float getHorizontalAngle(com.onyx.android.sdk.data.note.TouchPoint, com.onyx.android.sdk.data.note.TouchPoint);`
- `public static float getPointDistance(float, float, float, float);`
- `public static float[] getTransformRectPoints(android.graphics.RectF, android.graphics.Matrix);`
- `public static com.onyx.android.sdk.data.note.TouchPoint getIntersection(com.onyx.android.sdk.data.note.TouchPoint, com.onyx.android.sdk.data.note.TouchPoint, com.onyx.android.sdk.data.note.TouchPoint, com.onyx.android.sdk.data.note.TouchPoint);`
- `public static float[] renderPointArray(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public static float[] renderPointArray(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float);`
- `public static float[] realPointArray(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public static float[] realPointArray(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float);`
- `public static com.onyx.android.sdk.data.note.TouchPoint fromTinyPoint(com.onyx.android.sdk.data.point.TinyPoint);`
- `public static com.onyx.android.sdk.data.point.TinyPoint toTinyPoint(com.onyx.android.sdk.data.note.TouchPoint, long);`
- `public static void size2Tilt(com.onyx.android.sdk.data.point.TinyPoint, com.onyx.android.sdk.data.note.TouchPoint);`
- `public static void tilt2Size(com.onyx.android.sdk.data.note.TouchPoint, com.onyx.android.sdk.data.point.TinyPoint);`
- `public static int getTouchPointCoordinatesHashCode(com.onyx.android.sdk.data.note.TouchPoint, int, int);`
- `public static int computePointByteSize(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public static java.util.List<com.onyx.android.sdk.data.note.TouchPoint> getRectPoints(android.graphics.RectF);`
- `public static android.graphics.RectF getPointRectF(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public static java.util.List<com.onyx.android.sdk.data.note.TouchPoint> copyList(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public void set(com.onyx.android.sdk.data.note.TouchPoint);`
- `public com.onyx.android.sdk.data.note.TouchPoint transform(android.graphics.Matrix);`
- `public void normalize(com.onyx.android.sdk.data.PageInfo);`
- `public void origin(com.onyx.android.sdk.data.PageInfo);`
- `public com.onyx.android.sdk.data.note.TouchPoint clone();`
- `public com.onyx.android.sdk.data.note.TouchPoint copy();`
- `public java.lang.String toString();`
- `public boolean equalXY(com.onyx.android.sdk.data.note.TouchPoint);`
- `public boolean equals(java.lang.Object);`
- `public int hashCode();`
- `public com.onyx.android.sdk.base.data.TouchPoint transform(android.graphics.Matrix);`
- `public com.onyx.android.sdk.base.data.TouchPoint clone();`
- `public com.onyx.android.sdk.base.data.TouchPoint copy();`
- `public java.lang.Object clone() throws java.lang.CloneNotSupportedException;`


#### `com.onyx.android.sdk.rx.MultiThreadScheduler`

- `public final class com.onyx.android.sdk.rx.MultiThreadScheduler {`
- `public static final com.onyx.android.sdk.rx.MultiThreadScheduler INSTANCE;`
- `public static final io.reactivex.Scheduler scheduler();`
- `public static final io.reactivex.Scheduler newScheduler();`
- `public static io.reactivex.Scheduler newScheduler$default(com.onyx.android.sdk.rx.MultiThreadScheduler, java.lang.String, long, java.util.concurrent.TimeUnit, int, java.lang.Object);`
- `public final io.reactivex.Scheduler newScheduler(java.lang.String, long, java.util.concurrent.TimeUnit);`


#### `com.onyx.android.sdk.rx.ObservableHolder`

- `public class com.onyx.android.sdk.rx.ObservableHolder<T>`
- `public com.onyx.android.sdk.rx.ObservableHolder();`
- `public com.onyx.android.sdk.rx.ObservableHolder(io.reactivex.Observable<T>);`
- `public com.onyx.android.sdk.rx.ObservableHolder<T> onNext(T);`
- `public com.onyx.android.sdk.rx.ObservableHolder<T> onError(java.lang.Throwable);`
- `public void onComplete();`
- `public io.reactivex.Observable<T> getObservable();`
- `public io.reactivex.Observable<T> subscribeOn(io.reactivex.Scheduler);`
- `public io.reactivex.Observable<T> observeOn(io.reactivex.Scheduler);`
- `public com.onyx.android.sdk.rx.ObservableHolder<T> setDisposable(io.reactivex.disposables.Disposable);`
- `public void dispose();`
- `public boolean isDisposed();`


#### `com.onyx.android.sdk.rx.ObservableHolder$a`

- `public void subscribe(io.reactivex.ObservableEmitter<T>);`


#### `com.onyx.android.sdk.rx.RequestChain`

- `public class com.onyx.android.sdk.rx.RequestChain<T extends com.onyx.android.sdk.rx.RxRequest> extends com.onyx.android.sdk.rx.RxRequest`
- `public com.onyx.android.sdk.rx.RequestChain();`
- `public void execute() throws java.lang.Exception;`
- `public void setAbort();`
- `public void beforeExecute(com.onyx.android.sdk.rx.RxRequest) throws java.lang.Exception;`
- `public void afterExecute(com.onyx.android.sdk.rx.RxRequest) throws java.lang.Exception;`
- `public com.onyx.android.sdk.rx.RequestChain addRequest(T);`
- `public com.onyx.android.sdk.rx.RequestChain addRequestList(java.util.List<T>);`
- `public java.util.List<T> getRequestList();`


#### `com.onyx.android.sdk.rx.RetryWithDelayFunction`

- `public class com.onyx.android.sdk.rx.RetryWithDelayFunction implements io.reactivex.functions.Function<io.reactivex.Observable<? extends java.lang.Throwable>, io.reactivex.Observable<?>>`
- `public com.onyx.android.sdk.rx.RetryWithDelayFunction(int, int);`
- `public io.reactivex.Observable<?> apply(io.reactivex.Observable<? extends java.lang.Throwable>);`
- `public java.lang.Object apply(java.lang.Object) throws java.lang.Exception;`


#### `com.onyx.android.sdk.rx.RxAction`

- `public abstract class com.onyx.android.sdk.rx.RxAction<T extends com.onyx.android.sdk.rx.RxRequest>`
- `public com.onyx.android.sdk.rx.RxAction();`
- `public abstract void execute(com.onyx.android.sdk.rx.RxCallback<T>);`


#### `com.onyx.android.sdk.rx.RxBaseAction`

- `public abstract class com.onyx.android.sdk.rx.RxBaseAction<T>`
- `public com.onyx.android.sdk.rx.RxBaseAction();`
- `public static void init(android.content.Context);`
- `public static void setEnableBenchmark(boolean);`
- `public static void setReportTimeMs(int);`
- `public static android.content.Context getAppContext();`
- `public void setUseWakelock(boolean);`
- `public io.reactivex.Observable<T> build();`
- `public io.reactivex.Scheduler trampolineMainThread();`
- `public void onSubscribe(io.reactivex.disposables.Disposable);`
- `public boolean isDisposed();`
- `public void addDisposable(io.reactivex.disposables.Disposable);`
- `public void dispose();`
- `public void benchmarkStart();`
- `public boolean isWakeLockHeld();`
- `public io.reactivex.disposables.Disposable execute();`
- `public io.reactivex.Observable<T> buildFast();`
- `public com.onyx.android.sdk.rx.RxBaseAction<T> setActivityContext(android.content.Context);`
- `public android.content.Context getActivityContext();`
- `public android.app.Activity getActivity();`
- `public void setAbort();`
- `public boolean isAbort();`
- `public java.util.concurrent.atomic.AtomicBoolean getAbort();`
- `public io.reactivex.Scheduler getMainUIScheduler();`


#### `com.onyx.android.sdk.rx.RxBaseBenchmarkRequest`

- `public abstract class com.onyx.android.sdk.rx.RxBaseBenchmarkRequest extends com.onyx.android.sdk.rx.RxBaseRequest<kotlin.Unit>`
- `public com.onyx.android.sdk.rx.RxBaseBenchmarkRequest();`
- `public final com.onyx.android.sdk.utils.Benchmark getBenchmark();`
- `public final java.lang.Exception getException();`
- `public final void setException(java.lang.Exception);`
- `public final boolean getThrowException();`
- `public final void setThrowException(boolean);`
- `public final java.util.concurrent.atomic.AtomicBoolean getRunning();`
- `public final void execute();`
- `public final void disableThrowException();`
- `public final boolean hasException();`
- `public abstract void safelyExecuteImpl();`
- `public java.lang.Object execute();`


#### `com.onyx.android.sdk.rx.RxBaseGlobalWackLockAction`

- `public abstract class com.onyx.android.sdk.rx.RxBaseGlobalWackLockAction<T> extends com.onyx.android.sdk.rx.RxBaseAction<T>`
- `public com.onyx.android.sdk.rx.RxBaseGlobalWackLockAction();`


#### `com.onyx.android.sdk.rx.RxBaseRequest`

- `public abstract class com.onyx.android.sdk.rx.RxBaseRequest<T>`
- `public com.onyx.android.sdk.rx.RxBaseRequest();`
- `public static void init(android.content.Context);`
- `public static android.content.Context getAppContext();`
- `public abstract T execute() throws java.lang.Exception;`
- `public void setAbort();`
- `public boolean isAbort();`


#### `com.onyx.android.sdk.rx.RxCallback`

- `public abstract class com.onyx.android.sdk.rx.RxCallback<T> implements io.reactivex.Observer<T>`
- `public com.onyx.android.sdk.rx.RxCallback();`
- `public static <T> void onSubscribe(com.onyx.android.sdk.rx.RxCallback<T>, io.reactivex.disposables.Disposable);`
- `public static <T> void onNext(com.onyx.android.sdk.rx.RxCallback<T>, T);`
- `public static <T> void onError(com.onyx.android.sdk.rx.RxCallback<T>, java.lang.Throwable);`
- `public static <T> void onComplete(com.onyx.android.sdk.rx.RxCallback<T>);`
- `public static <T> void onFinally(com.onyx.android.sdk.rx.RxCallback<T>);`
- `public void onSubscribe(io.reactivex.disposables.Disposable);`
- `public abstract void onNext(T);`
- `public void onError(java.lang.Throwable);`
- `public void onComplete();`
- `public void onFinally();`


#### `com.onyx.android.sdk.rx.RxDebounce`

- `public class com.onyx.android.sdk.rx.RxDebounce implements io.reactivex.disposables.Disposable`
- `public com.onyx.android.sdk.rx.RxDebounce();`
- `public io.reactivex.Observable<java.lang.Boolean> subscribeDebounce(long, io.reactivex.functions.Consumer<java.lang.Boolean>, io.reactivex.functions.Consumer<java.lang.Boolean>);`
- `public void onNext(long);`
- `public void dispose();`
- `public boolean isDisposed();`


#### `com.onyx.android.sdk.rx.RxFilter`

- `public class com.onyx.android.sdk.rx.RxFilter<T> implements io.reactivex.disposables.Disposable`
- `public static final int DEFAULT_INTERVAL_DURATION;`
- `public com.onyx.android.sdk.rx.RxFilter();`
- `public void subscribeDebounce(long, io.reactivex.functions.Consumer<? super T>);`
- `public void subscribeThrottleLast(long, io.reactivex.functions.Consumer<? super T>);`
- `public void subscribeBuffer(int, io.reactivex.functions.Consumer<? super java.util.List<T>>);`
- `public void subscribeThrottleLast(io.reactivex.functions.Consumer<? super T>);`
- `public void subscribeThrottleFirst(long, io.reactivex.functions.Consumer<? super T>);`
- `public void subscribeThrottleFirst(io.reactivex.functions.Consumer<? super T>);`
- `public void subscribeThrottleFirst(long, io.reactivex.Scheduler, io.reactivex.functions.Consumer<? super T>);`
- `public void onNext(T);`
- `public void dispose();`
- `public boolean isDisposed();`


#### `com.onyx.android.sdk.rx.RxFilter$a`

- `public void subscribe(io.reactivex.ObservableEmitter<T>);`


#### `com.onyx.android.sdk.rx.RxFirstDebounceWithReset`

- `public final class com.onyx.android.sdk.rx.RxFirstDebounceWithReset<T> implements io.reactivex.disposables.Disposable {`
- `public static final com.onyx.android.sdk.rx.RxFirstDebounceWithReset$Companion Companion;`
- `public com.onyx.android.sdk.rx.RxFirstDebounceWithReset();`
- `public static final com.onyx.android.sdk.rx.RxFirstDebounceWithReset$a access$getStateMachine$p(com.onyx.android.sdk.rx.RxFirstDebounceWithReset);`
- `public static final io.reactivex.Observable access$processImmediately(com.onyx.android.sdk.rx.RxFirstDebounceWithReset, java.lang.Object, long, java.util.concurrent.TimeUnit, kotlin.jvm.functions.Function1);`
- `public static final io.reactivex.Observable access$processWithDebounce(com.onyx.android.sdk.rx.RxFirstDebounceWithReset, java.lang.Object, long, java.util.concurrent.TimeUnit, kotlin.jvm.functions.Function1);`
- `public final void subscribeForUI(long, java.util.concurrent.TimeUnit, kotlin.jvm.functions.Function1<? super T, kotlin.Unit>);`
- `public final void subscribe(long, java.util.concurrent.TimeUnit, kotlin.jvm.functions.Function1<? super T, ? extends io.reactivex.Observable<?>>);`
- `public final void sendEvent(T);`
- `public void dispose();`
- `public boolean isDisposed();`


#### `com.onyx.android.sdk.rx.RxFirstDebounceWithReset$Companion`

- `public final class com.onyx.android.sdk.rx.RxFirstDebounceWithReset$Companion {`
- `public com.onyx.android.sdk.rx.RxFirstDebounceWithReset$Companion(kotlin.jvm.internal.DefaultConstructorMarker);`


#### `com.onyx.android.sdk.rx.RxFirstDebounceWithReset$a`

- `public com.onyx.android.sdk.rx.RxFirstDebounceWithReset$a(com.onyx.android.sdk.rx.RxFirstDebounceWithReset);`
- `public final java.util.concurrent.atomic.AtomicReference<T> a();`
- `public final java.util.concurrent.atomic.AtomicReference<T> c();`
- `public final java.util.concurrent.atomic.AtomicLong b();`
- `public final void a(java.util.concurrent.atomic.AtomicLong);`
- `public final void d(T);`
- `public final void c(T);`
- `public final void a(T);`
- `public final kotlin.Pair<java.lang.Long, java.util.concurrent.TimeUnit> a(long, java.util.concurrent.TimeUnit);`
- `public final boolean d();`
- `public final boolean b(T);`


#### `com.onyx.android.sdk.rx.RxFirstDebounceWithReset$b`

- `public final java.lang.Boolean a(T);`
- `public java.lang.Object invoke(java.lang.Object);`


#### `com.onyx.android.sdk.rx.RxFirstDebounceWithReset$c`

- `public final io.reactivex.Observable<? extends java.lang.Object> a(T);`
- `public java.lang.Object invoke(java.lang.Object);`


#### `com.onyx.android.sdk.rx.RxFirstDebounceWithReset$d`

- `public final java.lang.Boolean a(T);`
- `public java.lang.Object invoke(java.lang.Object);`


#### `com.onyx.android.sdk.rx.RxFirstDebounceWithReset$e`

- `public final io.reactivex.Observable<? extends java.lang.Object> a(T);`
- `public java.lang.Object invoke(java.lang.Object);`


#### `com.onyx.android.sdk.rx.RxFirstDebounceWithReset$f`

- `public final io.reactivex.Observable<?> a(T);`
- `public java.lang.Object invoke(java.lang.Object);`


#### `com.onyx.android.sdk.rx.RxFrameHandler`

- `public final class com.onyx.android.sdk.rx.RxFrameHandler {`
- `public static final com.onyx.android.sdk.rx.RxFrameHandler INSTANCE;`
- `public final io.reactivex.subjects.PublishSubject<java.lang.Long> getPublishSubject();`
- `public final void start();`
- `public final void stop();`
- `public final void checkLeakOnDestroy();`


#### `com.onyx.android.sdk.rx.RxManager`

- `public final class com.onyx.android.sdk.rx.RxManager {`
- `public static void setEnableBenchmarkDebug(boolean);`
- `public static void setReportTime(int);`
- `public io.reactivex.Scheduler getObserveOn();`
- `public io.reactivex.Scheduler getSubscribeOn();`
- `public android.content.Context getAppContext();`
- `public void setUseWakelock(boolean);`
- `public final io.reactivex.Scheduler getObserveScheduler();`
- `public <T extends com.onyx.android.sdk.rx.RxRequest> void enqueue(T, com.onyx.android.sdk.rx.RxCallback<T>);`
- `public <T extends com.onyx.android.sdk.rx.RxRequest> void enqueueList(com.onyx.android.sdk.rx.RxCallback<T>);`
- `public <T extends com.onyx.android.sdk.rx.RxRequest> com.onyx.android.sdk.rx.RxManager append(T);`
- `public void shutdown();`
- `public <T extends com.onyx.android.sdk.rx.RxRequest> void concat(java.util.List<T>, com.onyx.android.sdk.rx.RxCallback<T>);`
- `public <T1 extends com.onyx.android.sdk.rx.RxRequest, T2 extends com.onyx.android.sdk.rx.RxRequest, T3 extends com.onyx.android.sdk.rx.RxRequest, T4> void zip3(T1, T2, T3, io.reactivex.functions.Function3<T1, T2, T3, T4>, com.onyx.android.sdk.rx.RxCallback<T4>);`
- `public <T extends com.onyx.android.sdk.rx.RxRequest, R> void zip(java.util.List<T>, io.reactivex.functions.Function<java.lang.Object[], R>, com.onyx.android.sdk.rx.RxCallback<T>);`
- `public <T extends com.onyx.android.sdk.rx.RxRequest> io.reactivex.Observable<T> create(T);`
- `public boolean isEnableBenchmarkDebug();`


#### `com.onyx.android.sdk.rx.RxManager$Builder`

- `public final class com.onyx.android.sdk.rx.RxManager$Builder {`
- `public com.onyx.android.sdk.rx.RxManager$Builder();`
- `public static void initAppContext(android.content.Context);`
- `public static com.onyx.android.sdk.rx.RxManager fromScheduler(io.reactivex.Scheduler);`
- `public static com.onyx.android.sdk.rx.RxManager sharedSingleThreadManager();`
- `public static com.onyx.android.sdk.rx.RxManager sharedMultiThreadManager();`
- `public static com.onyx.android.sdk.rx.RxManager newSingleThreadManager();`
- `public static com.onyx.android.sdk.rx.RxManager newSingleThreadManager(java.lang.String);`
- `public static com.onyx.android.sdk.rx.RxManager newMultiThreadManager();`
- `public com.onyx.android.sdk.rx.RxManager$Builder subscribeOn(io.reactivex.Scheduler);`
- `public com.onyx.android.sdk.rx.RxManager$Builder observeOn(io.reactivex.Scheduler);`
- `public com.onyx.android.sdk.rx.RxManager build();`


#### `com.onyx.android.sdk.rx.RxManager$ThreadPoolIdentifier`

- `public class com.onyx.android.sdk.rx.RxManager$ThreadPoolIdentifier`
- `public static final java.lang.String DEFAULT;`
- `public static final java.lang.String DB;`
- `public static final java.lang.String DATA;`
- `public static final java.lang.String EXTRACT;`
- `public static final java.lang.String FS;`
- `public static final java.lang.String CLOUD;`
- `public com.onyx.android.sdk.rx.RxManager$ThreadPoolIdentifier();`


#### `com.onyx.android.sdk.rx.RxManager$a`

- `public T a() throws java.lang.Exception;`
- `public java.lang.Object call() throws java.lang.Exception;`


#### `com.onyx.android.sdk.rx.RxManager$b`

- `public io.reactivex.ObservableSource<T> apply(io.reactivex.Observable<T>);`


#### `com.onyx.android.sdk.rx.RxManager$b$a`

- `public void run() throws java.lang.Exception;`


#### `com.onyx.android.sdk.rx.RxManager$b$b`

- `public void a(io.reactivex.disposables.Disposable) throws java.lang.Exception;`
- `public void accept(java.lang.Object) throws java.lang.Exception;`


#### `com.onyx.android.sdk.rx.RxManager$c`

- `public void onNext(T);`
- `public void onError(java.lang.Throwable);`
- `public void onComplete();`


#### `com.onyx.android.sdk.rx.RxRequest`

- `public abstract class com.onyx.android.sdk.rx.RxRequest`
- `public static final java.lang.String DEFAULT_IDENTIFIER;`
- `public com.onyx.android.sdk.rx.RxRequest();`
- `public abstract void execute() throws java.lang.Exception;`
- `public void setAbort();`
- `public boolean isAbort();`
- `public android.content.Context getContext();`
- `public com.onyx.android.sdk.rx.RxRequest setContext(android.content.Context);`
- `public java.lang.String getIdentifier();`


#### `com.onyx.android.sdk.rx.RxScheduler`

- `public final class com.onyx.android.sdk.rx.RxScheduler {`
- `public static final com.onyx.android.sdk.rx.RxScheduler$Companion Companion;`
- `public io.reactivex.Scheduler subscribeOn;`
- `public io.reactivex.Scheduler observeOn;`
- `public com.onyx.android.sdk.rx.RxScheduler();`
- `public static final com.onyx.android.sdk.rx.RxScheduler sharedSingleThreadManager();`
- `public static final com.onyx.android.sdk.rx.RxScheduler sharedMultiThreadManager();`
- `public static final com.onyx.android.sdk.rx.RxScheduler newSingleThreadManager(java.lang.String);`
- `public static final com.onyx.android.sdk.rx.RxScheduler newMultiThreadManager();`
- `public static final com.onyx.android.sdk.rx.RxScheduler newSingleThreadManager();`
- `public final io.reactivex.Scheduler getSubscribeOn();`
- `public final void setSubscribeOn(io.reactivex.Scheduler);`
- `public final io.reactivex.Scheduler getObserveOn();`
- `public final void setObserveOn(io.reactivex.Scheduler);`
- `public final com.onyx.android.sdk.rx.RxScheduler subscribeOn(io.reactivex.Scheduler);`
- `public final com.onyx.android.sdk.rx.RxScheduler observeOn(io.reactivex.Scheduler);`
- `public final void shutdown();`


#### `com.onyx.android.sdk.rx.RxScheduler$Companion`

- `public final class com.onyx.android.sdk.rx.RxScheduler$Companion {`
- `public static com.onyx.android.sdk.rx.RxScheduler newSingleThreadManager$default(com.onyx.android.sdk.rx.RxScheduler$Companion, java.lang.String, int, java.lang.Object);`
- `public static com.onyx.android.sdk.rx.RxScheduler newMultiThreadManager$default(com.onyx.android.sdk.rx.RxScheduler$Companion, java.lang.String, long, int, java.lang.Object);`
- `public com.onyx.android.sdk.rx.RxScheduler$Companion(kotlin.jvm.internal.DefaultConstructorMarker);`
- `public final com.onyx.android.sdk.rx.RxScheduler sharedSingleThreadManager();`
- `public final com.onyx.android.sdk.rx.RxScheduler sharedMultiThreadManager();`
- `public final com.onyx.android.sdk.rx.RxScheduler newSingleThreadManager(java.lang.String);`
- `public final com.onyx.android.sdk.rx.RxScheduler newSingleThreadManager(java.lang.String, long);`
- `public final com.onyx.android.sdk.rx.RxScheduler newMultiThreadManager();`
- `public final com.onyx.android.sdk.rx.RxScheduler createScheduler(io.reactivex.Scheduler);`
- `public final com.onyx.android.sdk.rx.RxScheduler newMultiThreadManager(java.lang.String, long);`
- `public final com.onyx.android.sdk.rx.RxScheduler newSingleThreadManager();`


#### `com.onyx.android.sdk.rx.RxScroller`

- `public final class com.onyx.android.sdk.rx.RxScroller {`
- `public static final com.onyx.android.sdk.rx.RxScroller$Companion Companion;`
- `public static final boolean DEBUG_LOG;`
- `public com.onyx.android.sdk.rx.RxScroller();`
- `public final io.reactivex.Observable<com.onyx.android.sdk.rx.RxScrollerInfo> fling(com.onyx.android.sdk.rx.RxScrollerArgs);`
- `public final void stop();`


#### `com.onyx.android.sdk.rx.RxScroller$Companion`

- `public final class com.onyx.android.sdk.rx.RxScroller$Companion {`
- `public com.onyx.android.sdk.rx.RxScroller$Companion(kotlin.jvm.internal.DefaultConstructorMarker);`


#### `com.onyx.android.sdk.rx.RxScrollerArgs`

- `public final class com.onyx.android.sdk.rx.RxScrollerArgs {`
- `public com.onyx.android.sdk.rx.RxScrollerArgs();`
- `public final float getStartX();`
- `public final void setStartX(float);`
- `public final float getStartY();`
- `public final void setStartY(float);`
- `public final float getVelocityX();`
- `public final void setVelocityX(float);`
- `public final float getVelocityY();`
- `public final void setVelocityY(float);`
- `public final android.graphics.RectF getLimitRect();`
- `public final void setLimitRect(android.graphics.RectF);`
- `public final float getFrictionX();`
- `public final void setFrictionX(float);`
- `public final float getFrictionY();`
- `public final void setFrictionY(float);`
- `public final boolean isEnabledAbortFlingAnimation();`
- `public final void setEnabledAbortFlingAnimation(boolean);`
- `public final com.onyx.android.sdk.rx.RxScrollerArgs$FlingActionType getFlingActionType();`
- `public final void setFlingActionType(com.onyx.android.sdk.rx.RxScrollerArgs$FlingActionType);`
- `public final java.lang.String getDebugString();`


#### `com.onyx.android.sdk.rx.RxScrollerArgs$FlingActionType`

- `public final class com.onyx.android.sdk.rx.RxScrollerArgs$FlingActionType extends java.lang.Enum<com.onyx.android.sdk.rx.RxScrollerArgs$FlingActionType> {`
- `public static final com.onyx.android.sdk.rx.RxScrollerArgs$FlingActionType CONTINUOUS_SCROLL;`
- `public static final com.onyx.android.sdk.rx.RxScrollerArgs$FlingActionType NEXT_SCREEN;`
- `public static final com.onyx.android.sdk.rx.RxScrollerArgs$FlingActionType PREV_SCREEN;`
- `public static final com.onyx.android.sdk.rx.RxScrollerArgs$FlingActionType REBOUND_CURRENT_SCREEN;`
- `public static final com.onyx.android.sdk.rx.RxScrollerArgs$FlingActionType CONTINUOUS_SCROLL_IN_CURRENT_SCREEN;`
- `public static com.onyx.android.sdk.rx.RxScrollerArgs$FlingActionType[] values();`
- `public static com.onyx.android.sdk.rx.RxScrollerArgs$FlingActionType valueOf(java.lang.String);`


#### `com.onyx.android.sdk.rx.RxScrollerInfo`

- `public final class com.onyx.android.sdk.rx.RxScrollerInfo {`
- `public com.onyx.android.sdk.rx.RxScrollerInfo();`
- `public final boolean isXFinished();`
- `public final void setXFinished(boolean);`
- `public final boolean isYFinished();`
- `public final void setYFinished(boolean);`
- `public final float getNewX();`
- `public final void setNewX(float);`
- `public final float getNewY();`
- `public final void setNewY(float);`
- `public final void set(com.onyx.android.sdk.rx.RxScrollerInfo);`
- `public final boolean isFinished();`


#### `com.onyx.android.sdk.rx.RxUtils`

- `public class com.onyx.android.sdk.rx.RxUtils`
- `public com.onyx.android.sdk.rx.RxUtils();`
- `public static void runInIO(java.lang.Runnable);`
- `public static void runInIO(java.lang.Runnable, io.reactivex.functions.Consumer<java.lang.Object>);`
- `public static void runInComputation(java.lang.Runnable);`
- `public static io.reactivex.disposables.Disposable runInComputation(java.lang.Runnable, io.reactivex.functions.Consumer<java.lang.Object>);`
- `public static void runInUI(java.lang.Runnable);`
- `public static void postRunInUISafely(android.os.Handler, java.lang.Runnable);`
- `public static void run(java.lang.Runnable, io.reactivex.Scheduler);`
- `public static io.reactivex.disposables.Disposable run(java.lang.Runnable, io.reactivex.Scheduler, io.reactivex.functions.Consumer<java.lang.Object>);`
- `public static <T> void runWith(java.util.concurrent.Callable<T>, io.reactivex.functions.Consumer<T>, io.reactivex.Scheduler);`
- `public static <T> void runWithInComputation(java.util.concurrent.Callable<T>, io.reactivex.functions.Consumer<T>);`
- `public static void dispose(io.reactivex.disposables.Disposable);`
- `public static boolean isDisposed(io.reactivex.disposables.Disposable);`
- `public static io.reactivex.disposables.Disposable disposeInUiThread(io.reactivex.functions.Action);`
- `public static void switchToUIThreadDispose(io.reactivex.functions.Action);`
- `public static <T> void acceptItemSafety(io.reactivex.functions.Consumer<T>, T);`
- `public static <T, R> void acceptItemSafety(com.onyx.android.sdk.rx.RxUtils$Consumer2<T, R>, T, R);`
- `public static <T, R> R applyItemSafety(io.reactivex.functions.Function<T, R>, T);`
- `public static <T> boolean testSafety(io.reactivex.functions.Predicate<T>, T);`
- `public static <T> void done(io.reactivex.Observer<T>, T);`
- `public static io.reactivex.Observable<android.view.View> postDelayObservable(android.view.View, int);`
- `public static <T, R> io.reactivex.Observable<R> applyItemObservable(T, io.reactivex.Scheduler, io.reactivex.functions.Function<T, R>);`
- `public static <T> io.reactivex.Observable<java.util.List<T>> emptyListObservable();`
- `public static <T> io.reactivex.functions.Consumer<T> emptyConsumer();`


#### `com.onyx.android.sdk.rx.RxUtils$Consumer2`

- `public interface com.onyx.android.sdk.rx.RxUtils$Consumer2<T, R>`
- `public abstract void accept(T, R) throws java.lang.Exception;`


#### `com.onyx.android.sdk.rx.RxUtils$a`

- `public java.lang.Object call() throws java.lang.Exception;`


#### `com.onyx.android.sdk.rx.RxUtils$b`

- `public java.lang.Object call() throws java.lang.Exception;`


#### `com.onyx.android.sdk.rx.RxUtils$c`

- `public void run() throws java.lang.Exception;`


#### `com.onyx.android.sdk.rx.RxUtils$d`

- `public void run();`


#### `com.onyx.android.sdk.rx.SingleThreadScheduler`

- `public class com.onyx.android.sdk.rx.SingleThreadScheduler`
- `public static final long DEFAULT_KEEP_ALIVE_TIME_IN_SECOND;`
- `public static final long LONG_KEEP_ALIVE_TIME_IN_SECOND;`
- `public com.onyx.android.sdk.rx.SingleThreadScheduler();`
- `public static io.reactivex.Scheduler scheduler();`
- `public static io.reactivex.Scheduler newScheduler();`
- `public static io.reactivex.Scheduler newScheduler(java.lang.String);`
- `public static java.util.concurrent.ExecutorService newSingleThreadExecutor();`
- `public static java.util.concurrent.ExecutorService newSingleThreadExecutor(java.lang.String);`
- `public static java.util.concurrent.ExecutorService newSingleThreadExecutor(java.lang.String, long);`
- `public static java.util.concurrent.ThreadFactory newThreadFactory(java.lang.String);`


#### `com.onyx.android.sdk.rx.SingleThreadScheduler$a`

- `public java.lang.Thread newThread(java.lang.Runnable);`


#### `com.onyx.android.sdk.rx.ThreadPoolHolder`

- `public class com.onyx.android.sdk.rx.ThreadPoolHolder`
- `public com.onyx.android.sdk.rx.ThreadPoolHolder();`
- `public com.onyx.android.sdk.rx.RxManager getRxManager(android.content.Context, java.lang.String, boolean);`


#### `com.onyx.android.sdk.rx.rxbroadcast.RxBroadcast`

- `public class com.onyx.android.sdk.rx.rxbroadcast.RxBroadcast`
- `public static final java.lang.String TAG;`
- `public com.onyx.android.sdk.rx.rxbroadcast.RxBroadcast();`
- `public static io.reactivex.disposables.Disposable connectivityChangeWithTimeOut(android.content.Context, com.onyx.android.sdk.rx.RxCallback<java.lang.Boolean>);`
- `public static io.reactivex.disposables.Disposable connectivityChange(android.content.Context, com.onyx.android.sdk.rx.RxCallback<java.lang.Boolean>);`


#### `com.onyx.android.sdk.rx.rxbroadcast.RxBroadcast$a`

- `public void a(java.lang.Boolean) throws java.lang.Exception;`
- `public void accept(java.lang.Object) throws java.lang.Exception;`


#### `com.onyx.android.sdk.rx.rxbroadcast.RxBroadcast$b`

- `public void a(java.lang.Throwable) throws java.lang.Exception;`
- `public void accept(java.lang.Object) throws java.lang.Exception;`


#### `com.onyx.android.sdk.rx.rxbroadcast.RxBroadcast$c`

- `public void a(java.lang.Boolean) throws java.lang.Exception;`
- `public void accept(java.lang.Object) throws java.lang.Exception;`


#### `com.onyx.android.sdk.rx.rxbroadcast.RxBroadcast$d`

- `public void a(java.lang.Throwable) throws java.lang.Exception;`
- `public void accept(java.lang.Object) throws java.lang.Exception;`


#### `com.onyx.android.sdk.rx.rxbroadcast.RxBroadcastChangeObservable`

- `public class com.onyx.android.sdk.rx.rxbroadcast.RxBroadcastChangeObservable extends io.reactivex.Observable<android.content.Intent>`
- `public com.onyx.android.sdk.rx.rxbroadcast.RxBroadcastChangeObservable(java.lang.String);`
- `public com.onyx.android.sdk.rx.rxbroadcast.RxBroadcastChangeObservable(java.util.List<java.lang.String>);`
- `public com.onyx.android.sdk.rx.rxbroadcast.RxBroadcastChangeObservable setReceiverFlags(int);`


#### `com.onyx.android.sdk.rx.rxbroadcast.RxBroadcastChangeObservable$a`

- `public com.onyx.android.sdk.rx.rxbroadcast.RxBroadcastChangeObservable$a(io.reactivex.Observer<? super android.content.Intent>, android.content.Context);`
- `public void onReceive(android.content.Context, android.content.Intent);`
- `public void dispose();`
- `public boolean isDisposed();`


#### `com.onyx.android.sdk.rx.rxbroadcast.RxConditionBroadcastObservable`

- `public class com.onyx.android.sdk.rx.rxbroadcast.RxConditionBroadcastObservable extends com.onyx.android.sdk.rx.rxbroadcast.RxBroadcastChangeObservable`
- `public com.onyx.android.sdk.rx.rxbroadcast.RxConditionBroadcastObservable(java.lang.String, com.onyx.android.sdk.rx.rxbroadcast.RxConditionBroadcastObservable$CheckConditionListener);`


#### `com.onyx.android.sdk.rx.rxbroadcast.RxConditionBroadcastObservable$CheckConditionListener`

- `public interface com.onyx.android.sdk.rx.rxbroadcast.RxConditionBroadcastObservable$CheckConditionListener`
- `public abstract boolean check(android.content.Intent);`


#### `com.onyx.android.sdk.rx.rxbroadcast.RxConditionBroadcastObservable$a`

- `public void onReceive(android.content.Context, android.content.Intent);`


#### `com.onyx.android.sdk.rx.rxbroadcast.RxConnectivityChangeObservable`

- `public class com.onyx.android.sdk.rx.rxbroadcast.RxConnectivityChangeObservable extends io.reactivex.Observable<java.lang.Boolean>`
- `public com.onyx.android.sdk.rx.rxbroadcast.RxConnectivityChangeObservable(android.content.Context);`


#### `com.onyx.android.sdk.rx.rxbroadcast.RxConnectivityChangeObservable$a`

- `public com.onyx.android.sdk.rx.rxbroadcast.RxConnectivityChangeObservable$a(io.reactivex.Observer<? super java.lang.Boolean>, android.content.Context);`
- `public void onReceive(android.content.Context, android.content.Intent);`
- `public void dispose();`
- `public boolean isDisposed();`


#### `com.onyx.android.sdk.rx.rxcontentobserver.RxContentObserver`

- `public class com.onyx.android.sdk.rx.rxcontentobserver.RxContentObserver`
- `public com.onyx.android.sdk.rx.rxcontentobserver.RxContentObserver();`
- `public io.reactivex.Observable<java.lang.String> buildForString(android.content.Context, com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType, java.lang.String);`
- `public io.reactivex.Observable<java.lang.Integer> buildForInt(android.content.Context, com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType, java.lang.String);`
- `public io.reactivex.Observable<java.lang.Float> buildForFloat(android.content.Context, com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType, java.lang.String);`
- `public void dispose();`


#### `com.onyx.android.sdk.rx.rxcontentobserver.RxContentObserver$a`

- `public void onChange(boolean);`


#### `com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType`

- `public final class com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType extends java.lang.Enum<com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType> {`
- `public static final com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType GLOBAL;`
- `public static final com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType SYSTEM;`
- `public static final com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType SECURE;`
- `public static com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType[] values();`
- `public static com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType valueOf(java.lang.String);`
- `public static android.net.Uri getUri(com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType, java.lang.String);`
- `public static int getInt(android.content.ContentResolver, com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType, java.lang.String) throws java.lang.Exception;`
- `public static int getInt(android.content.ContentResolver, com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType, java.lang.String, int) throws java.lang.Exception;`
- `public static java.lang.String getString(android.content.ContentResolver, com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType, java.lang.String) throws java.lang.Exception;`
- `public static boolean putInt(android.content.ContentResolver, com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType, java.lang.String, int) throws java.lang.Exception;`
- `public static float getFloat(android.content.ContentResolver, com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType, java.lang.String) throws java.lang.Exception;`
- `public static float getFloat(android.content.ContentResolver, com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType, java.lang.String, float) throws java.lang.Exception;`
- `public static boolean putFloat(android.content.ContentResolver, com.onyx.android.sdk.rx.rxcontentobserver.RxContentSettingsType, java.lang.String, float) throws java.lang.Exception;`


## onyxsdk-device-1.3.3.aar

- Included classes/interfaces: 7

#### `com.onyx.android.sdk.api.device.epd.EPDMode`

- `public final class com.onyx.android.sdk.api.device.epd.EPDMode extends java.lang.Enum<com.onyx.android.sdk.api.device.epd.EPDMode> {`
- `public static final com.onyx.android.sdk.api.device.epd.EPDMode FULL;`
- `public static final com.onyx.android.sdk.api.device.epd.EPDMode AUTO;`
- `public static final com.onyx.android.sdk.api.device.epd.EPDMode TEXT;`
- `public static final com.onyx.android.sdk.api.device.epd.EPDMode AUTO_PART;`
- `public static final com.onyx.android.sdk.api.device.epd.EPDMode AUTO_BLACK_WHITE;`
- `public static final com.onyx.android.sdk.api.device.epd.EPDMode AUTO_A2;`
- `public static final com.onyx.android.sdk.api.device.epd.EPDMode EPD_REGLA;`
- `public static com.onyx.android.sdk.api.device.epd.EPDMode[] values();`
- `public static com.onyx.android.sdk.api.device.epd.EPDMode valueOf(java.lang.String);`


#### `com.onyx.android.sdk.api.device.epd.EpdController`

- `public abstract class com.onyx.android.sdk.api.device.epd.EpdController`
- `public static final int STROKE_STYLE_PENCIL;`
- `public static final int STROKE_STYLE_BRUSH;`
- `public static final int STROKE_STYLE_MARKER;`
- `public static final int STROKE_STYLE_NEO_BRUSH;`
- `public static final int STROKE_STYLE_CHARCOAL;`
- `public static int SCHEME_START;`
- `public static int SCHEME_NORMAL;`
- `public static int SCHEME_KEYBOARD;`
- `public static int SCHEME_SCRIBBLE;`
- `public static int SCHEME_APPLICATION_ANIMATION;`
- `public static int SCHEME_SYSTEM_ANIMATION;`
- `public static int SCHEME_END;`
- `public static final java.lang.String ENABLE_SYSTEM_CTP_ACTION;`
- `public static final java.lang.String RESET_SYSTEM_CTP_ACTION;`
- `public static final java.lang.String CTP_STATUS_CHANGE_ACTION;`
- `public static final float MAX_TOUCH_PRESSURE;`
- `public static void invalidate(android.view.View, com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static void invalidate(android.view.View, int, int, int, int, com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static void postInvalidate(android.view.View, com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static boolean enableScreenUpdate(android.view.View, boolean);`
- `public static boolean setViewDefaultUpdateMode(android.view.View, com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static com.onyx.android.sdk.api.device.epd.UpdateMode getViewDefaultUpdateMode(android.view.View);`
- `public static boolean isDeepGcMode(android.view.View);`
- `public static void useFastScheme();`
- `public static void resetUpdateMode(android.view.View);`
- `public static boolean resetViewUpdateMode(android.view.View);`
- `public static com.onyx.android.sdk.api.device.epd.UpdateMode getSystemDefaultUpdateMode();`
- `public static boolean setSystemDefaultUpdateMode(com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static boolean applyAppScopeUpdate(java.lang.String, boolean, boolean, com.onyx.android.sdk.api.device.epd.UpdateMode, int);`
- `public static boolean clearAppScopeUpdate();`
- `public static boolean clearAppScopeUpdate(boolean);`
- `public static boolean applyTransientUpdate(com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static boolean clearTransientUpdate(android.view.View, boolean);`
- `public static boolean clearTransientUpdate(boolean);`
- `public static boolean setDisplayScheme(int);`
- `public static boolean applySystemFastMode(boolean);`
- `public static com.onyx.android.sdk.api.device.epd.UpdateOption getAppScopeRefreshMode();`
- `public static boolean setAppScopeRefreshMode(com.onyx.android.sdk.api.device.epd.UpdateOption);`
- `public static void waitForUpdateFinished();`
- `public static void refreshScreen(android.view.View, com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static void refreshScreenRegion(android.view.View, int, int, int, int, com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static boolean supportRegal();`
- `public static boolean isRegalEnabled();`
- `public static void holdDisplay(boolean, com.onyx.android.sdk.api.device.epd.UpdateMode, int);`
- `public static void byPass(int);`
- `public static void setStrokeWidth(float);`
- `public static void setStrokeStyle(int);`
- `public static void setStrokeColor(int);`
- `public static void setScreenHandWritingPenState(android.view.View, int);`
- `public static boolean isValidPenState();`
- `public static int getPenState();`
- `public static void setScreenHandWritingRegionLimit(android.view.View, int, int, int, int);`
- `public static void setScreenHandWritingRegionMode(android.view.View, int);`
- `public static void setScreenHandWritingRegionLimit(android.view.View);`
- `public static void setScreenHandWritingRegionLimit(android.view.View, int[]);`
- `public static void setScreenHandWritingRegionLimit(android.view.View, android.graphics.Rect[]);`
- `public static void setScreenHandWritingRegionExclude(android.view.View, android.graphics.Rect[]);`
- `public static float startStroke(float, float, float, float, float, float);`
- `public static float addStrokePoint(float, float, float, float, float, float);`
- `public static float finishStroke(float, float, float, float, float, float);`
- `public static void enterScribbleMode(android.view.View);`
- `public static void leaveScribbleMode(android.view.View);`
- `public static void enablePost(android.view.View, int);`
- `public static void enablePost(int);`
- `public static void enterScribbleMode();`
- `public static void leaveScribbleMode();`
- `public static void resetEpdPost();`
- `public static void setPainterStyle(boolean, android.graphics.Paint$Style, android.graphics.Paint$Join, android.graphics.Paint$Cap);`
- `public static void moveTo(float, float, float);`
- `public static void moveTo(android.view.View, float, float, float);`
- `public static void moveTo(android.view.View, float, float, float, float);`
- `public static void lineTo(float, float, com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static void lineTo(android.view.View, float, float, com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static void lineTo(android.view.View, float, float, com.onyx.android.sdk.api.device.epd.UpdateMode, float);`
- `public static void quadTo(float, float, com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static void quadTo(android.view.View, float, float, com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static void quadTo(android.view.View, float, float, com.onyx.android.sdk.api.device.epd.UpdateMode, float);`
- `public static void penUp();`
- `public static void disableA2ForSpecificView(android.view.View);`
- `public static void enableA2ForSpecificView(android.view.View);`
- `public static void setWebViewContrastOptimize(android.webkit.WebView, boolean);`
- `public static android.graphics.Matrix getRawTouchPointToScreenMatrix();`
- `public static void mapToView(android.view.View, float[], float[]);`
- `public static void mapToEpd(android.view.View, float[], float[]);`
- `public static android.graphics.Rect mapToEpd(android.view.View, android.graphics.Rect);`
- `public static void mapFromRawTouchPoint(android.view.View, float[], float[]);`
- `public static void mapToRawTouchPoint(android.view.View, float[], float[]);`
- `public static android.graphics.RectF mapToRawTouchPoint(android.view.View, android.graphics.RectF);`
- `public static float getTouchWidth();`
- `public static float getTouchHeight();`
- `public static float getMaxTouchPressure();`
- `public static float getEpdWidth();`
- `public static float getEpdHeight();`
- `public static void enableRegal();`
- `public static void disableRegal();`
- `public static void enableColorCU();`
- `public static void disableColorCU();`
- `public static void enableNightMode();`
- `public static void disableNightMode();`
- `public static void enableNightMode(boolean);`
- `public static void disableNightMode(boolean);`
- `public static boolean isSupportNightMode();`
- `public static void setUpdListSize(int);`
- `public static void resetUpdListSize();`
- `public static boolean inSystemFastMode();`
- `public static boolean isInFastMode();`
- `public static void setAppCTPDisableRegion(android.content.Context, int[]);`
- `public static void setAppCTPDisableRegion(android.content.Context, android.graphics.Rect[]);`
- `public static void setAppCTPDisableRegion(android.content.Context, int[], int[]);`
- `public static void setAppCTPDisableRegion(android.content.Context, android.graphics.Rect[], android.graphics.Rect[]);`
- `public static boolean isCTPDisableRegion(android.content.Context);`
- `public static void appResetCTPDisableRegion(android.content.Context);`
- `public static void setSystemCTPDisableRegion(android.content.Context);`
- `public static void systemResetCTPDisableRegion(android.content.Context);`
- `public static void dumpCTPInfo(android.content.Context);`
- `public static boolean isCTPPowerOn();`
- `public static boolean isEMTPPowerOn();`
- `public static void powerCTP(boolean);`
- `public static void powerEMTP(boolean);`
- `public static void switchToA2Mode();`
- `public static void applyGammaCorrection(boolean, int);`
- `public static void applyMonoLevel(int);`
- `public static void applyColorFilter(int);`
- `public static void applyGCOnce();`
- `public static void setTrigger(int);`
- `public static void repaintEveryThing();`
- `public static void repaintEveryThing(com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static void fillWhiteOnWakeup(boolean, com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static void useGCForNewSurface(boolean);`
- `public static void setAutoSyncBufEnable(boolean);`
- `public static void handwritingRepaint(android.view.View, int, int, int, int);`
- `public static void handwritingRepaint(android.view.View, android.graphics.Rect);`
- `public static void setEpdTurbo(int);`
- `public static void resetEpd(android.content.Context);`


#### `com.onyx.android.sdk.api.device.epd.RefreshType`

- `public final class com.onyx.android.sdk.api.device.epd.RefreshType extends java.lang.Enum<com.onyx.android.sdk.api.device.epd.RefreshType> {`
- `public static final com.onyx.android.sdk.api.device.epd.RefreshType NORMAL_GC;`
- `public static final com.onyx.android.sdk.api.device.epd.RefreshType DEEP_GC;`
- `public static com.onyx.android.sdk.api.device.epd.RefreshType[] values();`
- `public static com.onyx.android.sdk.api.device.epd.RefreshType valueOf(java.lang.String);`


#### `com.onyx.android.sdk.api.device.epd.UpdateMode`

- `public final class com.onyx.android.sdk.api.device.epd.UpdateMode extends java.lang.Enum<com.onyx.android.sdk.api.device.epd.UpdateMode> {`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode None;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode DU;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode DU4;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode GU;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode GU_FAST;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode GC;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode GCC;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode DEEP_GC;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode ANIMATION;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode ANIMATION_QUALITY;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode ANIMATION_MONO;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode ANIMATION_X;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode GC4;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode REGAL;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode REGAL_D;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode REGAL_PLUS;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode DU_QUALITY;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateMode HAND_WRITING_REPAINT_MODE;`
- `public static com.onyx.android.sdk.api.device.epd.UpdateMode[] values();`
- `public static com.onyx.android.sdk.api.device.epd.UpdateMode valueOf(java.lang.String);`
- `public static com.onyx.android.sdk.api.device.epd.UpdateMode getTypeByName(java.lang.String);`


#### `com.onyx.android.sdk.api.device.epd.UpdateOption`

- `public final class com.onyx.android.sdk.api.device.epd.UpdateOption extends java.lang.Enum<com.onyx.android.sdk.api.device.epd.UpdateOption> {`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateOption NORMAL;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateOption FAST_QUALITY;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateOption REGAL;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateOption FAST;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateOption FAST_X;`
- `public static com.onyx.android.sdk.api.device.epd.UpdateOption[] values();`
- `public static com.onyx.android.sdk.api.device.epd.UpdateOption valueOf(java.lang.String);`


#### `com.onyx.android.sdk.api.device.epd.UpdatePolicy`

- `public final class com.onyx.android.sdk.api.device.epd.UpdatePolicy extends java.lang.Enum<com.onyx.android.sdk.api.device.epd.UpdatePolicy> {`
- `public static final com.onyx.android.sdk.api.device.epd.UpdatePolicy Automatic;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdatePolicy GUIntervally;`
- `public static com.onyx.android.sdk.api.device.epd.UpdatePolicy[] values();`
- `public static com.onyx.android.sdk.api.device.epd.UpdatePolicy valueOf(java.lang.String);`


#### `com.onyx.android.sdk.api.device.epd.UpdateScheme`

- `public final class com.onyx.android.sdk.api.device.epd.UpdateScheme extends java.lang.Enum<com.onyx.android.sdk.api.device.epd.UpdateScheme> {`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateScheme None;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateScheme SNAPSHOT;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateScheme QUEUE;`
- `public static final com.onyx.android.sdk.api.device.epd.UpdateScheme QUEUE_AND_MERGE;`
- `public static com.onyx.android.sdk.api.device.epd.UpdateScheme[] values();`
- `public static com.onyx.android.sdk.api.device.epd.UpdateScheme valueOf(java.lang.String);`


## onyxsdk-pen-1.5.2.aar

- Included classes/interfaces: 55

#### `com.onyx.android.sdk.pen.BallpointPenRenderWrapper`

- `public final class com.onyx.android.sdk.pen.BallpointPenRenderWrapper extends com.onyx.android.sdk.pen.NeoPenRenderWrapper {`
- `public static final com.onyx.android.sdk.pen.BallpointPenRenderWrapper$Companion Companion;`
- `public com.onyx.android.sdk.pen.BallpointPenRenderWrapper(com.onyx.android.sdk.pen.NeoPen, boolean, kotlin.jvm.internal.DefaultConstructorMarker);`


#### `com.onyx.android.sdk.pen.BallpointPenRenderWrapper$Companion`

- `public final class com.onyx.android.sdk.pen.BallpointPenRenderWrapper$Companion {`
- `public com.onyx.android.sdk.pen.BallpointPenRenderWrapper$Companion(kotlin.jvm.internal.DefaultConstructorMarker);`
- `public final com.onyx.android.sdk.pen.BallpointPenRenderWrapper create(com.onyx.android.sdk.pen.NeoPenConfig);`


#### `com.onyx.android.sdk.pen.BuildConfig`

- `public final class com.onyx.android.sdk.pen.BuildConfig {`
- `public static final boolean DEBUG;`
- `public static final java.lang.String LIBRARY_PACKAGE_NAME;`
- `public static final java.lang.String BUILD_TYPE;`
- `public com.onyx.android.sdk.pen.BuildConfig();`


#### `com.onyx.android.sdk.pen.CharcoalNeoPenRender`

- `public final class com.onyx.android.sdk.pen.CharcoalNeoPenRender extends com.onyx.android.sdk.pen.NeoPenRender {`
- `public com.onyx.android.sdk.pen.CharcoalNeoPenRender(com.onyx.android.sdk.pen.NeoPen);`


#### `com.onyx.android.sdk.pen.EpdPenManager`

- `public class com.onyx.android.sdk.pen.EpdPenManager`
- `public static final int PEN_STOP;`
- `public static final int PEN_START;`
- `public static final int PEN_DRAWING;`
- `public static final int PEN_PAUSE;`
- `public static final int PEN_ERASING;`
- `public com.onyx.android.sdk.pen.EpdPenManager();`
- `public static void moveTo(float, float, float);`
- `public static void quadTo(float, float, com.onyx.android.sdk.api.device.epd.UpdateMode);`
- `public static void setStrokeColor(int);`
- `public com.onyx.android.sdk.pen.EpdPenManager setHostView(android.view.View);`
- `public void startDrawing();`
- `public void resumeDrawing();`
- `public void pauseDrawing();`
- `public void quitDrawing();`
- `public void setStrokeStyle(int);`


#### `com.onyx.android.sdk.pen.NeoBrushPenWrapper`

- `public class com.onyx.android.sdk.pen.NeoBrushPenWrapper`
- `public com.onyx.android.sdk.pen.NeoBrushPenWrapper();`
- `public static java.util.List<com.onyx.android.sdk.data.note.TouchPoint> computeStrokePoints(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float, float);`
- `public static java.util.List<com.onyx.android.sdk.data.note.TouchPoint> computeStrokePoints(float, java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float);`
- `public static void drawStroke(android.graphics.Canvas, android.graphics.Paint, java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float, float, boolean);`


#### `com.onyx.android.sdk.pen.NeoCharcoalPenV2Wrapper`

- `public class com.onyx.android.sdk.pen.NeoCharcoalPenV2Wrapper`
- `public com.onyx.android.sdk.pen.NeoCharcoalPenV2Wrapper();`
- `public static com.onyx.android.sdk.pen.NeoRenderPoint[] computeStrokeRenderPoints(com.onyx.android.sdk.pen.PenRenderArgs, java.util.List<android.graphics.Bitmap>);`
- `public static float[] computeStrokePoints(com.onyx.android.sdk.pen.PenRenderArgs, java.util.ArrayList<android.graphics.Bitmap>);`
- `public static void drawNormalStroke(com.onyx.android.sdk.pen.PenRenderArgs);`
- `public static void drawBigStroke(com.onyx.android.sdk.pen.PenRenderArgs);`


#### `com.onyx.android.sdk.pen.NeoCharcoalPenWrapper`

- `public class com.onyx.android.sdk.pen.NeoCharcoalPenWrapper`
- `public com.onyx.android.sdk.pen.NeoCharcoalPenWrapper();`
- `public static com.onyx.android.sdk.pen.NeoRenderPoint[] computeStrokeRenderPoints(com.onyx.android.sdk.pen.PenRenderArgs, java.util.List<android.graphics.Bitmap>);`
- `public static float[] computeStrokePoints(com.onyx.android.sdk.pen.PenRenderArgs, java.util.List<android.graphics.Bitmap>);`
- `public static void drawNormalStroke(com.onyx.android.sdk.pen.PenRenderArgs);`
- `public static void drawNormalStrokeImpl(com.onyx.android.sdk.pen.PenRenderArgs);`
- `public static void drawBigStroke(com.onyx.android.sdk.pen.PenRenderArgs);`
- `public static void drawBigStrokeImpl(com.onyx.android.sdk.pen.PenRenderArgs);`


#### `com.onyx.android.sdk.pen.NeoFountainPenWrapper`

- `public class com.onyx.android.sdk.pen.NeoFountainPenWrapper`
- `public static final float MIN_FOUNTAIN_PEN_WIDTH;`
- `public com.onyx.android.sdk.pen.NeoFountainPenWrapper();`
- `public static void drawStroke(android.graphics.Canvas, android.graphics.Paint, java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float, float, float, boolean);`
- `public static java.util.List<com.onyx.android.sdk.data.note.TouchPoint> computeStrokePoints(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float, float, float);`
- `public static boolean hasPressure(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public static java.util.List<com.onyx.android.sdk.data.note.TouchPoint> computeStrokePoints(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float, float);`


#### `com.onyx.android.sdk.pen.NeoMarkerPenWrapper`

- `public class com.onyx.android.sdk.pen.NeoMarkerPenWrapper`
- `public com.onyx.android.sdk.pen.NeoMarkerPenWrapper();`
- `public static java.util.List<com.onyx.android.sdk.data.note.TouchPoint> computeStrokePoints(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float, float);`
- `public static java.util.List<com.onyx.android.sdk.data.note.TouchPoint> computeStrokePoints(float, java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float);`
- `public static void drawStroke(android.graphics.Canvas, android.graphics.Paint, java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float, boolean);`


#### `com.onyx.android.sdk.pen.NeoPenConfigWrapper`

- `public class com.onyx.android.sdk.pen.NeoPenConfigWrapper`
- `public static final int NEOPEN_PEN_TYPE_BRUSH;`
- `public static final int NEOPEN_PEN_TYPE_FOUNTAIN;`
- `public static final int NEOPEN_PEN_TYPE_MARKER;`
- `public static final int NEOPEN_PEN_TYPE_CHARCOAL;`
- `public static final int NEOPEN_PEN_TYPE_CHARCOAL_V2;`
- `public static final float TILT_SCALE_VALUE;`
- `public int type;`
- `public int color;`
- `public float width;`
- `public int rotateAngle;`
- `public boolean tiltEnabled;`
- `public float tiltScale;`
- `public float maxTouchPressure;`
- `public com.onyx.android.sdk.pen.NeoPenConfigWrapper();`
- `public static void initPenConfig(com.onyx.android.sdk.pen.NeoPenConfigWrapper, com.onyx.android.sdk.data.note.ShapeCreateArgs);`
- `public int getType();`
- `public com.onyx.android.sdk.pen.NeoPenConfigWrapper setType(int);`
- `public int getColor();`
- `public com.onyx.android.sdk.pen.NeoPenConfigWrapper setColor(int);`
- `public float getWidth();`
- `public com.onyx.android.sdk.pen.NeoPenConfigWrapper setWidth(float);`
- `public int getRotateAngle();`
- `public com.onyx.android.sdk.pen.NeoPenConfigWrapper setRotateAngle(int);`
- `public boolean isTiltEnabled();`
- `public com.onyx.android.sdk.pen.NeoPenConfigWrapper setTiltEnabled(boolean);`
- `public float getTiltScale();`
- `public com.onyx.android.sdk.pen.NeoPenConfigWrapper setTiltScale(float);`
- `public float getMaxTouchPressure();`
- `public com.onyx.android.sdk.pen.NeoPenConfigWrapper setMaxTouchPressure(float);`


#### `com.onyx.android.sdk.pen.NeoPenRender`

- `public class com.onyx.android.sdk.pen.NeoPenRender`
- `public static final com.onyx.android.sdk.pen.NeoPenRender$Companion Companion;`
- `public static final int POINT_LIST_BATCH_LIMIT;`
- `public static final int DEFAULT_POINT_COUNT_THRESHOLD;`
- `public com.onyx.android.sdk.pen.NeoPenRender(com.onyx.android.sdk.pen.NeoPen);`
- `public static kotlin.Pair onTouchDown$default(com.onyx.android.sdk.pen.NeoPenRender, com.onyx.android.sdk.base.data.TouchPoint, boolean, int, java.lang.Object);`
- `public static kotlin.Pair onTouchMove$default(com.onyx.android.sdk.pen.NeoPenRender, java.util.List, com.onyx.android.sdk.base.data.TouchPoint, boolean, int, java.lang.Object);`
- `public static kotlin.Pair onTouchDone$default(com.onyx.android.sdk.pen.NeoPenRender, com.onyx.android.sdk.base.data.TouchPoint, boolean, int, java.lang.Object);`
- `public final com.onyx.android.sdk.pen.NeoPen getNeoPen();`
- `public final int getPointCount();`
- `public final void setPointCount(int);`
- `public final int getPointCountThreshold();`
- `public final void setPointCountThreshold(int);`
- `public final java.util.ArrayList<kotlin.Pair<com.onyx.android.sdk.pen.PenResult, com.onyx.android.sdk.pen.PenResult>> getPenResults();`
- `public final void onTouchPointList(java.util.List<? extends com.onyx.android.sdk.base.data.TouchPoint>);`
- `public final float[] loadPenPointArrays();`
- `public final int[] loadPenPointSizeArrays();`
- `public void render(android.graphics.Canvas, android.graphics.Paint, java.util.List<? extends com.onyx.android.sdk.base.data.TouchPoint>);`
- `public void render(android.graphics.Canvas, android.graphics.Paint);`
- `public kotlin.Pair<com.onyx.android.sdk.pen.PenResult, com.onyx.android.sdk.pen.PenResult> onTouchDown(com.onyx.android.sdk.base.data.TouchPoint, boolean);`
- `public kotlin.Pair<com.onyx.android.sdk.pen.PenResult, com.onyx.android.sdk.pen.PenResult> onTouchMove(java.util.List<? extends com.onyx.android.sdk.base.data.TouchPoint>, com.onyx.android.sdk.base.data.TouchPoint, boolean);`
- `public kotlin.Pair<com.onyx.android.sdk.pen.PenResult, com.onyx.android.sdk.pen.PenResult> onTouchDone(com.onyx.android.sdk.base.data.TouchPoint, boolean);`
- `public final void onTouchData(com.onyx.android.sdk.geometry.data.TouchData);`
- `public final void destroyPen();`
- `public void reset();`
- `public void resetPredict();`


#### `com.onyx.android.sdk.pen.NeoPenRender$Companion`

- `public final class com.onyx.android.sdk.pen.NeoPenRender$Companion {`
- `public com.onyx.android.sdk.pen.NeoPenRender$Companion(kotlin.jvm.internal.DefaultConstructorMarker);`


#### `com.onyx.android.sdk.pen.NeoPenRenderWrapper`

- `public class com.onyx.android.sdk.pen.NeoPenRenderWrapper extends com.onyx.android.sdk.pen.NeoPenRender`
- `public com.onyx.android.sdk.pen.NeoPenRenderWrapper(com.onyx.android.sdk.pen.NeoPen, boolean);`
- `public void render(android.graphics.Canvas, android.graphics.Paint);`


#### `com.onyx.android.sdk.pen.NeoPenUtils`

- `public class com.onyx.android.sdk.pen.NeoPenUtils`
- `public com.onyx.android.sdk.pen.NeoPenUtils();`
- `public static java.util.List<com.onyx.android.sdk.data.note.TouchPoint> computeStrokePoints(int, java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float, float);`
- `public static java.util.ArrayList<com.onyx.android.sdk.data.note.TouchPoint> mapToPenCanvas(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, android.graphics.Matrix);`
- `public static com.onyx.android.sdk.pen.NeoRenderPoint[] mapFromPenCanvas(com.onyx.android.sdk.pen.NeoRenderPoint[], java.util.List<android.graphics.Bitmap>, android.graphics.Matrix);`
- `public static void readPointResult(kotlin.Pair<com.onyx.android.sdk.pen.PenResult, com.onyx.android.sdk.pen.PenResult>, java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public static void readPointResults(java.util.List<kotlin.Pair<com.onyx.android.sdk.pen.PenResult, com.onyx.android.sdk.pen.PenResult>>, java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public static void readTextureResult(kotlin.Pair<com.onyx.android.sdk.pen.PenResult, com.onyx.android.sdk.pen.PenResult>, java.util.List<android.graphics.Bitmap>, java.util.ArrayList<com.onyx.android.sdk.pen.NeoRenderPoint>);`


#### `com.onyx.android.sdk.pen.NeoPenWrapper`

- `public class com.onyx.android.sdk.pen.NeoPenWrapper`
- `public com.onyx.android.sdk.pen.NeoPenWrapper();`
- `public static boolean initPen(com.onyx.android.sdk.pen.NeoPenConfig);`
- `public static void destroyPen();`
- `public static com.onyx.android.sdk.pen.NeoRenderPoint[] onPenDown(com.onyx.android.sdk.data.note.TouchPoint);`
- `public static com.onyx.android.sdk.pen.NeoRenderPoint[] onPenMove(com.onyx.android.sdk.data.note.TouchPoint);`
- `public static com.onyx.android.sdk.pen.NeoRenderPoint[] onPenUp(com.onyx.android.sdk.data.note.TouchPoint);`
- `public static com.onyx.android.sdk.pen.NeoRenderPoint[] computeRenderPoints(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public static android.graphics.Bitmap[] getRenderedBitmaps();`


#### `com.onyx.android.sdk.pen.NeoRenderPoint`

- `public class com.onyx.android.sdk.pen.NeoRenderPoint`
- `public float x;`
- `public float y;`
- `public float size;`
- `public int bitmapIndex;`
- `public com.onyx.android.sdk.pen.NeoRenderPoint();`
- `public static com.onyx.android.sdk.pen.NeoRenderPoint create(float, float, float, int);`
- `public static com.onyx.android.sdk.pen.NeoRenderPoint create(com.onyx.android.sdk.pen.NeoRenderPoint);`


#### `com.onyx.android.sdk.pen.PenRenderArgs`

- `public class com.onyx.android.sdk.pen.PenRenderArgs`
- `public com.onyx.android.sdk.pen.PenRenderArgs();`
- `public android.graphics.Canvas getCanvas();`
- `public com.onyx.android.sdk.pen.PenRenderArgs setCanvas(android.graphics.Canvas);`
- `public com.onyx.android.sdk.pen.PenRenderArgs setContentRect(android.graphics.RectF);`
- `public android.graphics.RectF getContentRect();`
- `public com.onyx.android.sdk.pen.PenRenderArgs setTiltEnabled(boolean);`
- `public boolean isTiltEnabled();`
- `public android.graphics.Paint getPaint();`
- `public com.onyx.android.sdk.pen.PenRenderArgs setPaint(android.graphics.Paint);`
- `public java.util.List<com.onyx.android.sdk.data.note.TouchPoint> getPoints();`
- `public com.onyx.android.sdk.pen.PenRenderArgs setPoints(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public com.onyx.android.sdk.pen.PenRenderArgs setPenType(int);`
- `public int getPenType();`
- `public int getColor();`
- `public com.onyx.android.sdk.pen.PenRenderArgs setColor(int);`
- `public float getStrokeWidth();`
- `public com.onyx.android.sdk.pen.PenRenderArgs setStrokeWidth(float);`
- `public com.onyx.android.sdk.data.note.ShapeCreateArgs getCreateArgs();`
- `public com.onyx.android.sdk.pen.PenRenderArgs setCreateArgs(com.onyx.android.sdk.data.note.ShapeCreateArgs);`
- `public android.graphics.Matrix getScreenMatrix();`
- `public com.onyx.android.sdk.pen.PenRenderArgs setScreenMatrix(android.graphics.Matrix);`
- `public com.onyx.android.sdk.pen.PenRenderArgs setRenderMatrix(android.graphics.Matrix);`
- `public android.graphics.Matrix getRenderMatrix();`
- `public boolean isErase();`
- `public com.onyx.android.sdk.pen.PenRenderArgs setErase(boolean);`


#### `com.onyx.android.sdk.pen.PenUtils`

- `public class com.onyx.android.sdk.pen.PenUtils`
- `public static final float ERASE_EXTRA_STROKE_WIDTH;`
- `public com.onyx.android.sdk.pen.PenUtils();`
- `public static android.graphics.Bitmap ensurePenBitmapCreated(android.graphics.Rect);`
- `public static int getColorSafely(int);`
- `public static void drawStrokeByPointSize(android.graphics.Canvas, android.graphics.Paint, java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, boolean);`
- `public static java.util.ArrayList<com.onyx.android.sdk.data.note.TouchPoint> toTouchPoints(com.onyx.android.sdk.pen.NeoRenderPoint[]);`
- `public static float[] getPointArray(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float);`
- `public static double[] getPointDoubleArray(com.onyx.android.sdk.data.note.TouchPoint, float);`
- `public static double[] getPointDoubleArray(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>, float);`


#### `com.onyx.android.sdk.pen.PencilNeoPenRender`

- `public final class com.onyx.android.sdk.pen.PencilNeoPenRender extends com.onyx.android.sdk.pen.NeoPenRender {`
- `public com.onyx.android.sdk.pen.PencilNeoPenRender(com.onyx.android.sdk.pen.NeoPencilPen);`
- `public final int getBrushPointCount();`
- `public final void setBrushPointCount(int);`
- `public void render(android.graphics.Canvas, android.graphics.Paint, java.util.List<? extends com.onyx.android.sdk.base.data.TouchPoint>);`
- `public kotlin.Pair<com.onyx.android.sdk.pen.PenResult, com.onyx.android.sdk.pen.PenResult> onTouchDown(com.onyx.android.sdk.base.data.TouchPoint, boolean);`
- `public kotlin.Pair<com.onyx.android.sdk.pen.PenResult, com.onyx.android.sdk.pen.PenResult> onTouchMove(java.util.List<? extends com.onyx.android.sdk.base.data.TouchPoint>, com.onyx.android.sdk.base.data.TouchPoint, boolean);`
- `public kotlin.Pair<com.onyx.android.sdk.pen.PenResult, com.onyx.android.sdk.pen.PenResult> onTouchDone(com.onyx.android.sdk.base.data.TouchPoint, boolean);`
- `public void reset();`


#### `com.onyx.android.sdk.pen.RawInputCallback`

- `public abstract class com.onyx.android.sdk.pen.RawInputCallback`
- `public com.onyx.android.sdk.pen.RawInputCallback();`
- `public abstract void onBeginRawDrawing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public abstract void onEndRawDrawing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public abstract void onRawDrawingTouchPointMoveReceived(com.onyx.android.sdk.data.note.TouchPoint);`
- `public abstract void onRawDrawingTouchPointListReceived(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public abstract void onBeginRawErasing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public abstract void onEndRawErasing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public abstract void onRawErasingTouchPointMoveReceived(com.onyx.android.sdk.data.note.TouchPoint);`
- `public abstract void onRawErasingTouchPointListReceived(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public void onPenActive(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onPenUpRefresh(android.graphics.RectF);`


#### `com.onyx.android.sdk.pen.RawInputManager`

- `public class com.onyx.android.sdk.pen.RawInputManager`
- `public com.onyx.android.sdk.pen.RawInputManager();`
- `public void setRawInputCallback(com.onyx.android.sdk.pen.RawInputCallback);`
- `public void startRawInputReader();`
- `public void resumeRawInputReader();`
- `public void pauseRawInputReader();`
- `public void quitRawInputReader();`
- `public boolean isUseRawInput();`
- `public com.onyx.android.sdk.pen.RawInputManager setUseRawInput(boolean);`
- `public android.view.View getHostView();`
- `public com.onyx.android.sdk.pen.RawInputManager setHostView(android.view.View);`
- `public com.onyx.android.sdk.pen.RawInputManager setLimitRect(android.graphics.Rect, java.util.List<android.graphics.Rect>);`
- `public com.onyx.android.sdk.pen.RawInputManager setLimitRect(java.util.List<android.graphics.Rect>, java.util.List<android.graphics.Rect>);`
- `public com.onyx.android.sdk.pen.RawInputManager setLimitRect(java.util.List<android.graphics.Rect>);`
- `public com.onyx.android.sdk.pen.RawInputManager setExcludeRect(java.util.List<android.graphics.Rect>);`
- `public void setStrokeWidth(float);`
- `public void setStrokeColor(int);`
- `public void setSingleRegionMode();`
- `public void setMultiRegionMode();`
- `public void setPenUpRefreshTimeMs(int);`
- `public void setPenUpRefreshEnabled(boolean);`
- `public void setFilterRepeatMovePoint(boolean);`
- `public void setPostInputEvent(boolean);`
- `public void enableSideBtnErase(boolean);`
- `public void setHostViewScrollListenerEnabled(boolean);`
- `public void printTouchInfo();`


#### `com.onyx.android.sdk.pen.RawInputManager$a`

- `public void onBeginRawDrawing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onEndRawDrawing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawDrawingTouchPointMoveReceived(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawDrawingTouchPointListReceived(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public void onBeginRawErasing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onEndRawErasing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawErasingTouchPointMoveReceived(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawErasingTouchPointListReceived(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public void onPenActive(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onPenUpRefresh(android.graphics.RectF);`


#### `com.onyx.android.sdk.pen.RawInputReader`

- `public class com.onyx.android.sdk.pen.RawInputReader`
- `public com.onyx.android.sdk.pen.RawInputReader();`
- `public static void debugLog(boolean);`
- `public void setRawInputCallback(com.onyx.android.sdk.pen.RawInputCallback);`
- `public void setHostView(android.view.View);`
- `public android.view.View getHostView();`
- `public void start();`
- `public void resume();`
- `public void pause();`
- `public void quit();`
- `public boolean isFdValid();`
- `public void setStrokeWidth(float);`
- `public void setStrokeColor(int);`
- `public void setPenUpRefreshTimeMs(int);`
- `public void setPostInputEvent(boolean);`
- `public void setPenUpRefreshEnabled(boolean);`
- `public boolean isPenUpRefreshEnabled();`
- `public void setFilterRepeatMovePoint(boolean);`
- `public boolean isFilterRepeatMovePoint();`
- `public void setSingleRegionMode();`
- `public void setMultiRegionMode();`
- `public void enableSideBtnErase(boolean);`
- `public void setLimitRect(android.graphics.Rect);`
- `public void setLimitRect(java.util.List<android.graphics.Rect>);`
- `public void setExcludeRect(java.util.List<android.graphics.Rect>);`
- `public void onTouchPointReceived(float, float, int, int, int, boolean, boolean, boolean, int, long);`
- `public com.onyx.android.sdk.utils.EventBusHolder getEventBusHolder();`
- `public com.onyx.android.sdk.pen.data.TouchPointList detachTouchPointList();`
- `public boolean isErasing();`
- `public void printTouchInfo();`


#### `com.onyx.android.sdk.pen.RawInputReader$a`

- `public void run();`


#### `com.onyx.android.sdk.pen.RawInputReader$b`

- `public void a(java.lang.Long);`
- `public void onNext(java.lang.Object);`


#### `com.onyx.android.sdk.pen.RawInputReader$c`

- `public void a(java.lang.Long);`
- `public void onNext(java.lang.Object);`


#### `com.onyx.android.sdk.pen.TouchEventBus`

- `public class com.onyx.android.sdk.pen.TouchEventBus`
- `public static com.onyx.android.sdk.pen.TouchEventBus getInstance();`
- `public com.onyx.android.sdk.utils.EventBusHolder getEventBusHolder();`


#### `com.onyx.android.sdk.pen.TouchHelper`

- `public class com.onyx.android.sdk.pen.TouchHelper`
- `public static final int STROKE_STYLE_PENCIL;`
- `public static final int STROKE_STYLE_FOUNTAIN;`
- `public static final int STROKE_STYLE_MARKER;`
- `public static final int STROKE_STYLE_NEO_BRUSH;`
- `public static final int STROKE_STYLE_CHARCOAL;`
- `public static final int STROKE_STYLE_DASH;`
- `public static final int STROKE_STYLE_CHARCOAL_V2;`
- `public static final int STROKE_STYLE_SQUARE_PEN;`
- `public static final int FEATURE_APP_TOUCH_RENDER;`
- `public static final int FEATURE_SF_TOUCH_RENDER;`
- `public static final int FEATURE_APP_PEN_TOUCH_RENDER;`
- `public static final int FEATURE_ALL_TOUCH_RENDER;`
- `public static com.onyx.android.sdk.pen.TouchHelper create(android.view.View, com.onyx.android.sdk.pen.RawInputCallback);`
- `public static com.onyx.android.sdk.pen.TouchHelper create(android.view.View, boolean, com.onyx.android.sdk.pen.RawInputCallback);`
- `public static com.onyx.android.sdk.pen.TouchHelper create(android.view.View, int, com.onyx.android.sdk.pen.RawInputCallback);`
- `public static com.onyx.android.sdk.pen.TouchHelper create(android.view.View, int, com.onyx.android.sdk.pen.RawInputCallback, boolean);`
- `public static void register(java.lang.Object);`
- `public static void unregister(java.lang.Object);`
- `public static com.onyx.android.sdk.utils.EventBusHolder getEventBusHolder();`
- `public com.onyx.android.sdk.pen.TouchHelper bindHostView(android.view.View, com.onyx.android.sdk.pen.RawInputCallback);`
- `public android.view.View getHostView();`
- `public boolean onTouchEvent(android.view.MotionEvent);`
- `public com.onyx.android.sdk.pen.TouchHelper setStrokeStyle(int);`
- `public com.onyx.android.sdk.pen.TouchHelper setStrokeColor(int);`
- `public com.onyx.android.sdk.pen.TouchHelper setStrokeWidth(float);`
- `public com.onyx.android.sdk.pen.TouchHelper debugLog(boolean);`
- `public com.onyx.android.sdk.pen.TouchHelper setLimitRect(android.graphics.Rect, java.util.List<android.graphics.Rect>);`
- `public com.onyx.android.sdk.pen.TouchHelper setLimitRect(java.util.List<android.graphics.Rect>, java.util.List<android.graphics.Rect>);`
- `public com.onyx.android.sdk.pen.TouchHelper setLimitRect(java.util.List<android.graphics.Rect>);`
- `public com.onyx.android.sdk.pen.TouchHelper setExcludeRect(java.util.List<android.graphics.Rect>);`
- `public com.onyx.android.sdk.pen.TouchHelper openRawDrawing();`
- `public void closeRawDrawing();`
- `public com.onyx.android.sdk.pen.TouchHelper setRawDrawingEnabled(boolean);`
- `public boolean isRawDrawingInputEnabled();`
- `public boolean isRawDrawingRenderEnabled();`
- `public com.onyx.android.sdk.pen.TouchHelper setRawDrawingRenderEnabled(boolean);`
- `public com.onyx.android.sdk.pen.TouchHelper forceSetRawDrawingEnabled(boolean);`
- `public com.onyx.android.sdk.pen.TouchHelper setRawInputReaderEnable(boolean);`
- `public boolean isRawDrawingCreated();`
- `public com.onyx.android.sdk.pen.TouchHelper setSingleRegionMode();`
- `public com.onyx.android.sdk.pen.TouchHelper setMultiRegionMode();`
- `public void setPenUpRefreshTimeMs(int);`
- `public void setPenUpRefreshEnabled(boolean);`
- `public void setFilterRepeatMovePoint(boolean);`
- `public void setPostInputEvent(boolean);`
- `public void setTouchListenerEnabled(boolean);`
- `public void enableSideBtnErase(boolean);`
- `public void enableFingerTouch(boolean);`
- `public void onlyEnableFingerTouch(boolean);`
- `public void enableFingerTouchPressure(boolean);`
- `public void setFingerTouchPressure(float);`
- `public void printTouchInfo();`
- `public com.onyx.android.sdk.pen.TouchHelper setBrushRawDrawingEnabled(boolean);`
- `public com.onyx.android.sdk.pen.TouchHelper setEraserRawDrawingEnabled(boolean);`
- `public com.onyx.android.sdk.pen.TouchHelper setHostViewScrollListenerEnabled(boolean);`
- `public void resetPenDefaultRawDrawing();`
- `public void restartRawDrawing();`


#### `com.onyx.android.sdk.pen.data.ComputePointResult`

- `public final class com.onyx.android.sdk.pen.data.ComputePointResult {`
- `public com.onyx.android.sdk.pen.data.ComputePointResult();`
- `public final com.onyx.android.sdk.pen.NeoRenderPoint getNeoRenderPoint();`
- `public final void setNeoRenderPoint(com.onyx.android.sdk.pen.NeoRenderPoint);`
- `public final android.graphics.Bitmap getBitmap();`
- `public final void setBitmap(android.graphics.Bitmap);`


#### `com.onyx.android.sdk.pen.data.MirrorType`

- `public final class com.onyx.android.sdk.pen.data.MirrorType extends java.lang.Enum<com.onyx.android.sdk.pen.data.MirrorType> {`
- `public static final com.onyx.android.sdk.pen.data.MirrorType XAxisMirror;`
- `public static final com.onyx.android.sdk.pen.data.MirrorType YAxisMirror;`
- `public static com.onyx.android.sdk.pen.data.MirrorType[] values();`
- `public static com.onyx.android.sdk.pen.data.MirrorType valueOf(java.lang.String);`


#### `com.onyx.android.sdk.pen.data.RotateType`

- `public final class com.onyx.android.sdk.pen.data.RotateType extends java.lang.Enum<com.onyx.android.sdk.pen.data.RotateType> {`
- `public static final com.onyx.android.sdk.pen.data.RotateType FREEDOM;`
- `public static final com.onyx.android.sdk.pen.data.RotateType CW_90;`
- `public static final com.onyx.android.sdk.pen.data.RotateType CWW_90;`
- `public static com.onyx.android.sdk.pen.data.RotateType[] values();`
- `public static com.onyx.android.sdk.pen.data.RotateType valueOf(java.lang.String);`
- `public float getAngle();`


#### `com.onyx.android.sdk.pen.data.TouchPointList`

- `public class com.onyx.android.sdk.pen.data.TouchPointList implements java.io.Serializable,java.lang.Cloneable`
- `public com.onyx.android.sdk.pen.data.TouchPointList();`
- `public com.onyx.android.sdk.pen.data.TouchPointList(int);`
- `public com.onyx.android.sdk.pen.data.TouchPointList(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public com.onyx.android.sdk.pen.data.TouchPointList(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public static com.onyx.android.sdk.pen.data.TouchPointList detachPointList(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public static android.graphics.RectF getBoundingRect(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public final java.util.List<com.onyx.android.sdk.data.note.TouchPoint> getPoints();`
- `public final java.util.List<com.onyx.android.sdk.data.note.TouchPoint> getRenderPoints();`
- `public com.onyx.android.sdk.pen.data.TouchPointList setPoints(java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public com.onyx.android.sdk.data.note.TouchPoint first();`
- `public com.onyx.android.sdk.data.note.TouchPoint last();`
- `public int size();`
- `public com.onyx.android.sdk.data.note.TouchPoint get(int);`
- `public void add(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void add(int, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void addAll(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public void addAll(int, java.util.List<com.onyx.android.sdk.data.note.TouchPoint>);`
- `public com.onyx.android.sdk.pen.data.TouchPointList addAll(java.util.List<com.onyx.android.sdk.data.point.TinyPoint>);`
- `public java.util.List<com.onyx.android.sdk.data.point.TinyPoint> detachPointList();`
- `public java.util.List<com.onyx.android.sdk.data.point.TinyPoint> toTinyPointList();`
- `public java.util.Iterator<com.onyx.android.sdk.data.note.TouchPoint> iterator();`
- `public com.onyx.android.sdk.pen.data.TouchPointList applyMatrix(android.graphics.Matrix);`
- `public com.onyx.android.sdk.pen.data.TouchPointList cloneMatrixPoints(android.graphics.Matrix);`
- `public void scaleAllPoints(float);`
- `public void scaleAllPoints(float, float);`
- `public void translateAllPoints(float, float);`
- `public void rotateAllPoints(float, android.graphics.PointF);`
- `public void mirrorAllPoints(com.onyx.android.sdk.pen.data.MirrorType, float);`
- `public com.onyx.android.sdk.pen.data.TouchPointList clone();`
- `public boolean isEmpty();`
- `public void clear();`
- `public java.lang.Object clone() throws java.lang.CloneNotSupportedException;`


#### `com.onyx.android.sdk.pen.event.PenActiveEvent`

- `public class com.onyx.android.sdk.pen.event.PenActiveEvent`
- `public com.onyx.android.sdk.pen.event.PenActiveEvent(com.onyx.android.sdk.data.note.TouchPoint);`
- `public com.onyx.android.sdk.data.note.TouchPoint getPoint();`


#### `com.onyx.android.sdk.pen.event.PenDeactivateEvent`

- `public class com.onyx.android.sdk.pen.event.PenDeactivateEvent`
- `public com.onyx.android.sdk.pen.event.PenDeactivateEvent(com.onyx.android.sdk.data.note.TouchPoint);`
- `public com.onyx.android.sdk.data.note.TouchPoint getPoint();`


#### `com.onyx.android.sdk.pen.event.PenDownPointLostEvent`

- `public final class com.onyx.android.sdk.pen.event.PenDownPointLostEvent {`
- `public com.onyx.android.sdk.pen.event.PenDownPointLostEvent();`


#### `com.onyx.android.sdk.pen.extension.TouchPointListKt`

- `public final class com.onyx.android.sdk.pen.extension.TouchPointListKt {`
- `public static final android.graphics.RectF getBoundingRect(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public static final java.util.List<com.onyx.android.sdk.data.note.TouchPoint> filterByGridSpacing(java.util.List<? extends com.onyx.android.sdk.data.note.TouchPoint>, float);`
- `public static final android.graphics.RectF calculateBoundingRect(java.util.List<? extends com.onyx.android.sdk.data.note.TouchPoint>, float);`


#### `com.onyx.android.sdk.pen.multiview.BaseViewWatcher`

- `public abstract class com.onyx.android.sdk.pen.multiview.BaseViewWatcher<T>`
- `public com.onyx.android.sdk.pen.multiview.BaseViewWatcher();`
- `public java.util.List<java.lang.ref.WeakReference<T>> getWatchedObjects();`
- `public boolean add(T);`
- `public boolean add(java.util.List<T>);`
- `public boolean remove(T);`
- `public boolean remove(java.util.List<T>);`
- `public boolean contains(T);`
- `public void clear();`
- `public abstract java.util.List<android.graphics.Rect> getRects();`
- `public abstract boolean hasVisibleObject();`
- `public boolean isAllInvisible();`
- `public int getContainerViewScreenX();`
- `public int getContainerViewScreenY();`
- `public com.onyx.android.sdk.pen.multiview.BaseViewWatcher<T> setContainerViewScreenX(int);`
- `public com.onyx.android.sdk.pen.multiview.BaseViewWatcher<T> setContainerViewScreenY(int);`


#### `com.onyx.android.sdk.pen.multiview.DialogWatcher`

- `public class com.onyx.android.sdk.pen.multiview.DialogWatcher extends com.onyx.android.sdk.pen.multiview.BaseViewWatcher<android.app.Dialog>`
- `public com.onyx.android.sdk.pen.multiview.DialogWatcher();`
- `public boolean hasVisibleObject();`
- `public java.util.List<android.graphics.Rect> getRects();`
- `public void clear();`


#### `com.onyx.android.sdk.pen.multiview.LimitViewInfo`

- `public class com.onyx.android.sdk.pen.multiview.LimitViewInfo`
- `public com.onyx.android.sdk.pen.multiview.LimitViewInfo(android.view.View, com.onyx.android.sdk.pen.RawInputCallback);`
- `public android.view.View getView();`
- `public com.onyx.android.sdk.pen.RawInputCallback getCallback();`
- `public com.onyx.android.sdk.pen.multiview.LimitViewInfo setCallback(com.onyx.android.sdk.pen.RawInputCallback);`
- `public java.util.List<android.graphics.Rect> getLimitRectList();`
- `public com.onyx.android.sdk.pen.multiview.LimitViewInfo setLimitRectList(java.util.List<android.graphics.Rect>);`
- `public java.util.List<android.graphics.Rect> getContainerLimitRectList();`
- `public com.onyx.android.sdk.pen.multiview.LimitViewInfo initLimitRect();`
- `public com.onyx.android.sdk.pen.multiview.LimitViewInfo setContainerViewScreenXY(int, int);`
- `public com.onyx.android.sdk.data.note.TouchPoint offSetXY(com.onyx.android.sdk.data.note.TouchPoint);`
- `public com.onyx.android.sdk.pen.data.TouchPointList offSetXY(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public boolean contains(android.graphics.Rect, com.onyx.android.sdk.data.note.TouchPoint);`
- `public boolean contains(com.onyx.android.sdk.data.note.TouchPoint);`
- `public boolean intersect(android.graphics.Rect);`
- `public android.graphics.Rect getRefreshRectInView(android.graphics.Rect);`
- `public android.graphics.Rect getViewInContainerRect();`


#### `com.onyx.android.sdk.pen.multiview.MultiViewTouchHelper`

- `public class com.onyx.android.sdk.pen.multiview.MultiViewTouchHelper`
- `public com.onyx.android.sdk.pen.multiview.MultiViewTouchHelper(android.view.View, int);`
- `public static void setDebug(boolean);`
- `public com.onyx.android.sdk.pen.multiview.MultiViewTouchHelper bindContainerView(android.view.View);`
- `public java.lang.Class getTag();`
- `public void clearActiveLimitViewInfo(boolean);`
- `public void refreshLimitRect();`
- `public void add(com.onyx.android.sdk.pen.multiview.LimitViewInfo);`
- `public void remove(com.onyx.android.sdk.pen.multiview.LimitViewInfo);`
- `public void clear();`
- `public com.onyx.android.sdk.pen.TouchHelper getTouchHelper();`
- `public void addExcludeView(java.util.List<android.view.View>);`
- `public void addExcludeView(android.view.View);`
- `public void removeExcludeView(android.view.View);`
- `public void removeExcludeView(java.util.List<android.view.View>);`


#### `com.onyx.android.sdk.pen.multiview.MultiViewTouchHelper$a`

- `public void onBeginRawDrawing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onEndRawDrawing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawDrawingTouchPointMoveReceived(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawDrawingTouchPointListReceived(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public void onBeginRawErasing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onEndRawErasing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawErasingTouchPointMoveReceived(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawErasingTouchPointListReceived(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public void onPenActive(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onPenUpRefresh(android.graphics.RectF);`


#### `com.onyx.android.sdk.pen.multiview.PopupWindowWatcher`

- `public class com.onyx.android.sdk.pen.multiview.PopupWindowWatcher extends com.onyx.android.sdk.pen.multiview.BaseViewWatcher<android.widget.PopupWindow>`
- `public com.onyx.android.sdk.pen.multiview.PopupWindowWatcher();`
- `public boolean hasVisibleObject();`
- `public java.util.List<android.graphics.Rect> getRects();`


#### `com.onyx.android.sdk.pen.multiview.SubFloatMenuViewWatcher`

- `public class com.onyx.android.sdk.pen.multiview.SubFloatMenuViewWatcher extends com.onyx.android.sdk.pen.multiview.BaseViewWatcher<android.view.View>`
- `public com.onyx.android.sdk.pen.multiview.SubFloatMenuViewWatcher();`
- `public java.util.List<android.graphics.Rect> getRects();`
- `public boolean hasVisibleObject();`


#### `com.onyx.android.sdk.pen.multiview.ViewWatcher`

- `public class com.onyx.android.sdk.pen.multiview.ViewWatcher extends com.onyx.android.sdk.pen.multiview.BaseViewWatcher<android.view.View>`
- `public com.onyx.android.sdk.pen.multiview.ViewWatcher();`
- `public java.util.List<android.graphics.Rect> getRects();`
- `public boolean hasVisibleObject();`


#### `com.onyx.android.sdk.pen.style.StrokeStyle`

- `public class com.onyx.android.sdk.pen.style.StrokeStyle`
- `public static final int PENCIL;`
- `public static final int FOUNTAIN;`
- `public static final int MARKER;`
- `public static final int NEO_BRUSH;`
- `public static final int CHARCOAL;`
- `public static final int DASH;`
- `public static final int CHARCOAL_V2;`
- `public static final int SQUARE_PEN;`
- `public com.onyx.android.sdk.pen.style.StrokeStyle();`


#### `com.onyx.android.sdk.pen.touch.AppInputCallback`

- `public abstract class com.onyx.android.sdk.pen.touch.AppInputCallback`
- `public com.onyx.android.sdk.pen.touch.AppInputCallback();`
- `public abstract void onBeginRawDrawing(android.view.MotionEvent, boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public abstract void onEndRawDrawing(android.view.MotionEvent, boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public abstract void onRawDrawingTouchPointMoveReceived(com.onyx.android.sdk.data.note.TouchPoint);`
- `public abstract void onRawDrawingTouchPointListReceived(android.view.MotionEvent, com.onyx.android.sdk.pen.data.TouchPointList);`
- `public void onBeginRawErasing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onEndRawErasing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawErasingTouchPointMoveReceived(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawErasingTouchPointListReceived(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public void onPenActive(com.onyx.android.sdk.data.note.TouchPoint);`


#### `com.onyx.android.sdk.pen.touch.AppPenTouchRender`

- `public final class com.onyx.android.sdk.pen.touch.AppPenTouchRender extends com.onyx.android.sdk.pen.touch.AppTouchRender {`
- `public com.onyx.android.sdk.pen.touch.AppPenTouchRender(android.view.View, com.onyx.android.sdk.pen.RawInputCallback);`
- `public void bindHostView(android.view.View, com.onyx.android.sdk.pen.RawInputCallback);`
- `public void setDrawingRenderEnabled(boolean);`
- `public void closeDrawing();`


#### `com.onyx.android.sdk.pen.touch.AppPenTouchRender$a`

- `public static final com.onyx.android.sdk.pen.touch.AppPenTouchRender$a a;`
- `public final com.onyx.android.sdk.pen.EpdPenManager a();`
- `public java.lang.Object invoke();`


#### `com.onyx.android.sdk.pen.touch.AppTouchInputReader`

- `public class com.onyx.android.sdk.pen.touch.AppTouchInputReader`
- `public com.onyx.android.sdk.pen.touch.AppTouchInputReader(com.onyx.android.sdk.pen.touch.AppInputCallback);`
- `public com.onyx.android.sdk.pen.touch.AppTouchInputReader setStrokeWidth(float);`
- `public float getStrokeWidth();`
- `public void setLimitRectList(android.view.View, java.util.List<android.graphics.Rect>);`
- `public void setExcludeRectList(android.view.View, java.util.List<android.graphics.Rect>);`
- `public void setEnableFingerTouch(boolean);`
- `public void setOnlyEnableFingerTouch(boolean);`
- `public void setFingerTouchPressure(float);`
- `public void setEnableFingerTouchPressure(boolean);`
- `public boolean processMotionEvent(android.view.MotionEvent);`


#### `com.onyx.android.sdk.pen.touch.AppTouchRender`

- `public class com.onyx.android.sdk.pen.touch.AppTouchRender implements com.onyx.android.sdk.pen.touch.TouchRender`
- `public com.onyx.android.sdk.pen.touch.AppTouchRender(android.view.View, com.onyx.android.sdk.pen.RawInputCallback);`
- `public void bindHostView(android.view.View, com.onyx.android.sdk.pen.RawInputCallback);`
- `public boolean onTouchEvent(android.view.MotionEvent);`
- `public android.view.View getHostView();`
- `public void setStrokeStyle(int);`
- `public void setStrokeColor(int);`
- `public void setInputReaderEnable(boolean);`
- `public void setSingleRegionMode();`
- `public void setMultiRegionMode();`
- `public void setPenUpRefreshTimeMs(int);`
- `public void setPenUpRefreshEnabled(boolean);`
- `public void setFilterRepeatMovePoint(boolean);`
- `public void setPostInputEvent(boolean);`
- `public void enableSideBtnErase(boolean);`
- `public void setLimitRect(java.util.List<android.graphics.Rect>);`
- `public void setLimitRect(android.graphics.Rect, java.util.List<android.graphics.Rect>);`
- `public void setLimitRect(java.util.List<android.graphics.Rect>, java.util.List<android.graphics.Rect>);`
- `public void setExcludeRect(java.util.List<android.graphics.Rect>);`
- `public void openDrawing();`
- `public void closeDrawing();`
- `public void setDrawingRenderEnabled(boolean);`
- `public void setBrushRawDrawingEnabled(boolean);`
- `public void setEraserRawDrawingEnabled(boolean);`
- `public void setStrokeWidth(float);`
- `public void debugLog(boolean);`
- `public void enableFingerTouch(boolean);`
- `public void onlyEnableFingerTouch(boolean);`
- `public void setHostViewScrollListenerEnabled(boolean);`
- `public void enableFingerTouchPressure(boolean);`
- `public void setFingerTouchPressure(float);`
- `public void printTouchInfo();`
- `public void setTouchListenerEnabled(boolean);`


#### `com.onyx.android.sdk.pen.touch.AppTouchRender$a`

- `public void onBeginRawDrawing(android.view.MotionEvent, boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onEndRawDrawing(android.view.MotionEvent, boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawDrawingTouchPointMoveReceived(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawDrawingTouchPointListReceived(android.view.MotionEvent, com.onyx.android.sdk.pen.data.TouchPointList);`


#### `com.onyx.android.sdk.pen.touch.SFTouchRender`

- `public class com.onyx.android.sdk.pen.touch.SFTouchRender implements com.onyx.android.sdk.pen.touch.TouchRender`
- `public com.onyx.android.sdk.pen.touch.SFTouchRender(android.view.View, com.onyx.android.sdk.pen.RawInputCallback);`
- `public static com.onyx.android.sdk.pen.touch.TouchRender create(android.view.View, com.onyx.android.sdk.pen.RawInputCallback);`
- `public void bindHostView(android.view.View, com.onyx.android.sdk.pen.RawInputCallback);`
- `public android.view.View getHostView();`
- `public void setStrokeStyle(int);`
- `public void setStrokeColor(int);`
- `public void setStrokeWidth(float);`
- `public void debugLog(boolean);`
- `public void setLimitRect(android.graphics.Rect, java.util.List<android.graphics.Rect>);`
- `public void setLimitRect(java.util.List<android.graphics.Rect>, java.util.List<android.graphics.Rect>);`
- `public void setLimitRect(java.util.List<android.graphics.Rect>);`
- `public void setExcludeRect(java.util.List<android.graphics.Rect>);`
- `public void openDrawing();`
- `public void closeDrawing();`
- `public void setDrawingRenderEnabled(boolean);`
- `public void setBrushRawDrawingEnabled(boolean);`
- `public void setEraserRawDrawingEnabled(boolean);`
- `public void setInputReaderEnable(boolean);`
- `public void setSingleRegionMode();`
- `public void setMultiRegionMode();`
- `public void setPenUpRefreshTimeMs(int);`
- `public void setPenUpRefreshEnabled(boolean);`
- `public void setFilterRepeatMovePoint(boolean);`
- `public void setPostInputEvent(boolean);`
- `public void enableSideBtnErase(boolean);`
- `public void enableFingerTouch(boolean);`
- `public void onlyEnableFingerTouch(boolean);`
- `public void setHostViewScrollListenerEnabled(boolean);`
- `public void setFingerTouchPressure(float);`
- `public void printTouchInfo();`
- `public void enableFingerTouchPressure(boolean);`
- `public void setTouchListenerEnabled(boolean);`


#### `com.onyx.android.sdk.pen.touch.SFTouchRender$b`

- `public void onBeginRawDrawing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onEndRawDrawing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawDrawingTouchPointMoveReceived(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawDrawingTouchPointListReceived(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public void onBeginRawErasing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onEndRawErasing(boolean, com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawErasingTouchPointMoveReceived(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onRawErasingTouchPointListReceived(com.onyx.android.sdk.pen.data.TouchPointList);`
- `public void onPenActive(com.onyx.android.sdk.data.note.TouchPoint);`
- `public void onPenUpRefresh(android.graphics.RectF);`


#### `com.onyx.android.sdk.pen.touch.TouchRender`

- `public interface com.onyx.android.sdk.pen.touch.TouchRender`
- `public abstract void bindHostView(android.view.View, com.onyx.android.sdk.pen.RawInputCallback);`
- `public abstract android.view.View getHostView();`
- `public default boolean onTouchEvent(android.view.MotionEvent);`
- `public abstract void setStrokeStyle(int);`
- `public abstract void setStrokeColor(int);`
- `public abstract void setStrokeWidth(float);`
- `public abstract void debugLog(boolean);`
- `public abstract void setLimitRect(android.graphics.Rect, java.util.List<android.graphics.Rect>);`
- `public abstract void setLimitRect(java.util.List<android.graphics.Rect>, java.util.List<android.graphics.Rect>);`
- `public abstract void setLimitRect(java.util.List<android.graphics.Rect>);`
- `public abstract void setExcludeRect(java.util.List<android.graphics.Rect>);`
- `public abstract void openDrawing();`
- `public abstract void closeDrawing();`
- `public abstract void setDrawingRenderEnabled(boolean);`
- `public abstract void setBrushRawDrawingEnabled(boolean);`
- `public abstract void setEraserRawDrawingEnabled(boolean);`
- `public abstract void setInputReaderEnable(boolean);`
- `public abstract void setSingleRegionMode();`
- `public abstract void setMultiRegionMode();`
- `public abstract void setPenUpRefreshTimeMs(int);`
- `public abstract void setPenUpRefreshEnabled(boolean);`
- `public abstract void setFilterRepeatMovePoint(boolean);`
- `public abstract void setPostInputEvent(boolean);`
- `public abstract void enableSideBtnErase(boolean);`
- `public abstract void enableFingerTouch(boolean);`
- `public abstract void onlyEnableFingerTouch(boolean);`
- `public abstract void setTouchListenerEnabled(boolean);`
- `public abstract void setHostViewScrollListenerEnabled(boolean);`
- `public abstract void enableFingerTouchPressure(boolean);`
- `public abstract void setFingerTouchPressure(float);`
- `public abstract void printTouchInfo();`


## Summary

- Total included classes/interfaces: 126


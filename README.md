# shared-futures
A **Java** library for reusing *concurrent* **Future** calls.

## Motivation
Ever been in the situation where you are developing a highly concurrent Java application that you want to support a high volume of requests per second?<br/>
If so, you might be familiar with conceps such as Servlet 3.0 async support and the Future interface.

A *Future* represents the result of an asynchronous computation, which could be anything from a simple task, to a complex one including external calls.<br/>
In highly concurrent and stateless REST API applications, each independant request often uses multiple resources, and in most cases, the internal tasks of the endpoints that fulfill each specific request could be parallelized using *Futures*.

With libraries such as Java 8 CompletableFuture, Spring's ListenableFuture, Guava's ListenableFuture, Spotify's Trickle, and Netflix's RxJava the general recipe for creating a high level **Task**, such as fulfilling a resquest, could be seen as **Graph** of *Future* calls that are linked one another through callbacks maximizing parallelism of execution. Each *Future* being an identifiable **Sub Task**, for instance an external call (to another REST API, a database, etc), that could be potentially reused, by another high level **Task** that is concurrently being executed. The higher the concurrency, the higher the potential of a *Sub Task* of bien reusable.

The following code depicts this scenario in a synchronous fashion for an ecommerce-like application where the "/product/{name}" endpoint fetches a product page information for a given user. The data returned, *ProductInfo*, includes product metadata, related products, other recommended products for the user, and the current's user shopping cart items.

~~~java
@Path(value = "product/{name}", method = Method.GET)
public ProductInfo getProductInfo(@PathParam String name, @Context User user) {
    
    Product product = productService.getProduct(name);
    
    List<Product> related = productService.getRelated(product);
    
    List<Product> recommended = productService.getRecommended(product, user);
    
    ShoppingCart cart = userService.getShoppingCart(user);
    
    return new ProductInfo(product, related, recommended, cart);
}
~~~ 

The asynchronous code using Java 8 CompletableFutures, asuming that each service method has an 'async' counterpart version that returns a CompletableFuture, would look like this, asumming also that our REST framework, has been wired to support Future return types in our endpoints, triggering Servlet 3.0 async support capabilities:

~~~java
@Path(value = "product/{name}", method = Method.GET)
public CompletableFuture<ProductInfo> getProductInfo(@PathParam String name, @Context User user) {
    
    CompletableFuture<Product>> product = productService.getProductAsync(name);
   
    CompletableFuture<List<Product>> related = product.thenCompose(productService::getRelatedAsync);
    
    CompletableFuture<List<Product>> recommended = product.thenCompose( 
                                                       prd -> user.thenCompose( 
                                                           usr -> productService.getRecommendedAsync(prd, usr)));
    
    CompletableFuture<ShoppingCart> cart = user.thenCompose(userService::getShoppingCartAsync);
    
    return product.thenCompose(
               prd -> related.thenCompose(
                   rlt -> recommended.thenCompose(
                       rcmd -> cart.thenApply(
                           crt -> new ProductInfo(prd, rlt, rcmd, crt)))));
}
~~~

Notice that none of the calls in the above calls are blocking, so as a result of executing the method, the resulting CompletableFuture<ProductInfo> is produced almost instantly, without waiting for the productService or userService methods to return any data. Instead, the resulting CompletableFuture<ProductInfo> could be seen as **Graph** of *Future* calls, and a final transformation into a ProductInfo object, thus maximizing parallelism for such endpoint.

In a high traffic environment, multiple calls to the same endpoint are being executed over and over again, and since the stateless nature of REST APIs don't store sessions or other state objects, the external calls ta each endpoint uses, are being executed over and over again. One solution to alleviate resource usage, increase performance, and increase throughput, is **Caching**. A practice well known and commonly used in REST APIs to *save* the state of certain external calls, often expensive, for data that doesn't change too often. Or if the data changes and it needs to be effectively evicted from Cache, when the data changes. In any case, not all external calls are *Cacheable*, for different reasons (Specially if data changes and we don't have away of effectively know when it changes), or Caching could be occurring on the external service itself. In this cases, where **Caching** is not an option, is where this library may effectively increase concurrency, by allowing the endpoints to share **in-flight Future Sub Tasks** of the same characteristics.

In the presented example, multiple requests to the same product info endpoint could be happening at the same time. In those cases, for each request that is currently being processed, it would make sense if all the concurrent requests to a given product, share the same `CompletableFuture<Product> product` and `CompletableFuture<List<Product>> related` futures, but different `CompletableFuture<List<Product>> recommended>` and `CompletableFuture<ShoppingCart>` futures, since this last two most likely will be happening for different users on each concurrent call.

## How This Library Works?

This library works by storing potentially reusable Future instances in a common data structure, for the period on which they are being executed. Once the Futures are completed, they are removed from the common data structure. Each future is identified by a String key, that is either generated or provided by the user. This depends on whether the developer is using the library "manually", or by annotating methods with the `@SharedFuture` annotation.<br/>
When the `@SharedFuture` annotation is used on a method, the key is generated based on the method parameters values. The library provides support for all primitive types, primitive Wrappers, Collection, and String. For custome data types, developers need to make those types implemented the `SharedFutureKey` interface, to provide a the Key value.

## Getting Started

1. Included the latest version of shared-futures into your project:

	~~~xml
	<dependency>
	  <groupId>com.ulisesbocchio</groupId>
	  <artifactId>shared-futures</artifactId>
	  <version>0.1</version>
	</dependency>
	~~~

2. Include the `@nableSharedFutures` annotation in your Spring Application Context JavaConfig class:

	~~~java
	@Configuration
	@EnableSharedFutures
	public static class ApplicationBeans {
	    ...
	}
	~~~

	The `@EnableSharedFutures` annotation imports the `EnableSharedFuturesAutoConfiguration` class which mainly consists of the following configuration:

	~~~java
	@Configuration
	@EnableAspectJAutoProxy
	public class EnableSharedFuturesAutoConfiguration {

		@Bean
		public SharedFuturesAspect enableSharedFuturesAspect() {
			return new SharedFuturesAspect();
		}

	    ...
	}
	~~~

3. Annotate your CompletableFuture, Spring's ListenableFuture, or Guava's ListenableFuture returning method with @SharedFuture

	~~~java
	@SharedFuture
	CompletableFuture<Product> getProductAsync(String name);
	~~~


4. Make sure parameter types are supported and implement `SharedFutureKey` in those custom types that aren't supported by default.

	For instance, for the following Shared Future to work:
	
	~~~java
	@SharedFuture
	CompletableFuture<ShoppingCart> getShoppingCartAsync(User user);
	~~~
	
	The User class needs to implement the `SharedFutureKey` interface, for instance, providing the user ID as key:
	
	~~~java
	public class User implements SharedFutureKey {
		
		...
		
		@Override
		public String getKey() {
			return this.userId;
		}
	}
	~~~

## Memory Footpring and Performance

This Library's memory footprint is really tiny, explicitly whatever memory takes to store a reference to each Shared Future being currently executed, plus its key. Futures, and their keys, are removed immediatly after they complete, whether they complete successfully or exceptionally.<br/>
In terms of performance, the penalty for having to add the Future to the common data structure before it is actually returned is compare to that of storing storing an object in a Key Indexed structure, which is normally constant. The extra excecution time that it takes to remove the Future from the common data structure when it finishes doesn't have an impact in response time, since it's doned asynchronously.
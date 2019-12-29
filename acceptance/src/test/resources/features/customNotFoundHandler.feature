@notFoundHandler @standalone
Feature: Custom Not found handler

Scenario: Not found handler with. Full request
	When I start a service "#serviceFuture" with module "com.simplyti.service.NotFoundFullHandlerModule"
	Then I check that "#serviceFuture" is success
	When I send a "GET /notfound" getting "#response"
	Then I check that "#response" has status code 404
	Then I check that "#response" is equals to "This is a custom handler"
	
Scenario: Not found handler. Http content stream
	When I start a service "#serviceFuture" with module "com.simplyti.service.NotFoundHandlerModule"
	Then I check that "#serviceFuture" is success
	When I send a "GET /notfound" getting "#response"
	Then I check that "#response" has status code 404
	Then I check that "#response" is equals to "This is a custom handler"
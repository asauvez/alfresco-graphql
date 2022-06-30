# GraphQL endpoint for Alfresco

This is an ACS project for Alfresco SDK 4.0, tested against ACS 6.2.0 community.

To know more about GraphQL : https://graphql.org/

Declare the following endpoints:
- http://localhost:8080/alfresco/graphql : GraphQL endpoint read only.
- http://localhost:8080/alfresco/graphql_mutation : GraphQL endpoint that allows mutations.
- http://localhost:8080/alfresco/graphiql : UI to create queries. Test with that one.
- http://localhost:8080/alfresco/graphiql_mutation : UI to create queries and mutations.

Interesting files:
- The GraphQL schema:  [alfresco.graphqls](src/main/resources/alfresco/module/graphql/alfresco.graphqls)
- Sample query: [query.graphql](src/test/resources/query.graphql)
- Sample results: [queryResponse.json](src/test/resources/queryResponse.json)
- Sample mutations: [mutation.graphql](src/test/resources/mutation.graphql)

Code coverage: 86% instructions, 76% branches.

# Author

Made for fun and education by Adrien SAUVEZ, Alfresco expert @Smile company (https://www.smile.eu).
Special thanks for Covid 19 for having forced me to stay at home and therefore give me time to work on this project..

# Install

- `git clone git@github.com:asauvez/alfresco-graphql.git`
- `cd alfresco-graphql`
- `./run.sh build_start`
- http://localhost:8080/alfresco/graphiql
```
query {
  node {
    sitesHome {
      childByName(name: "swsdp") {
        title
        childByName(name: "documentLibrary") {
          childrenContains {
            nodeRef,
            name,
            title
          }
        }
      }
    }
  }
}
```

Can be integrated in an existing Alfresco by declaring:
```
<dependency>
	<groupId>fr.smile.alfresco.graphql</groupId>
	<artifactId>graphql</artifactId>
	<version>1.0-SNAPSHOT</version>
</dependency>
```
This was tested against a 6.2 version Community and Enterprise.


# TODO
- Workflow
	Test
		
	updateTask
		{{http://www.alfresco.org/model/bpm/1.0}status=Not Yet Started, {http://www.alfresco.org/model/bpm/1.0}comment=}
		Completed
	task/path .properties as dynamic
		task association

- Dependency as provided?
- New transaction on subquery : return proxy with CompletableFuture?
	
- Categories
- Dictionary

- getDefaultDataFetcher
	improve PropertyDataFetcher with argument

- Subscription
	https://www.graphql-java.com/documentation/v14/subscriptions/
		You need to use SubscriptionExecutionStrategy as your execution strategy as it has the support for the reactive-streams APIs.
	return SubmissionPublisher
	can be tested in graphiql
	on a node or global?


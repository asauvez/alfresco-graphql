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

Code coverage: 82% instructions, 74% branches.

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



# TODO
- Make run.sh test work again

- manage several reads of request.getInputStream() by RetryngTransaction?

locked (setValue: Boolean)
favorite(setValue: Boolean)
	
- Checkout
- Lance action sur Node. 
- Categories

- getDefaultDataFetcher
	improve PropertyDataFetcher with argument

- delete without trashbin
- Test on another project
- moveNode with variable
	byNodeRef(nodeRef: "...") { saveAsVariable("destination") }
	moveNode(variable: "destination")
	createLink(variable: "destination")	
- Subscription
	https://www.graphql-java.com/documentation/v14/subscriptions/
		You need to use SubscriptionExecutionStrategy as your execution strategy as it has the support for the reactive-streams APIs.
	return SubmissionPublisher
	can be tested in graphiql
	on a node or global?


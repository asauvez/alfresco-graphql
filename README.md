# GraphQL endpoint for Alfresco

This is an ACS project for Alfresco SDK 4.0.
To know more about GraphQL : https://graphql.org/

# Author

Made for fun and education by Adrien SAUVEZ, Alfresco expert @Smile company (https://www.smile.eu).
Special thanks for Covid 19 for having forced me to stay at home and therefore give me time to work on this project..

# Install

Run with `./run.sh build_start` or `./run.bat build_start` and verify that it

Declare the following endpoints:
- http://localhost:8080/alfresco/graphql : GraphQL endpoint read only.
- http://localhost:8080/alfresco/graphql_mutation : GraphQL endpoint that allows mutations.
- http://localhost:8080/alfresco/graphiql : UI to create queries. Test with that one.

Can be integrated in an existing Alfresco by declaring:
```
<dependency>
	<groupId>fr.smile.alfresco.graphql</groupId>
	<artifactId>graphql</artifactId>
</dependency>
```

# TODO
- FTS search
- jar in AMP ?
- gérer plusieurs consommation du flux par RetryngTransaction ?
- authority (currentUser, permission)
- integration tests
- query by path

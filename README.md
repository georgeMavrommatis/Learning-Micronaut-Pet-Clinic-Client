## Micronaut 4.8.3 Documentation

- [User Guide](https://docs.micronaut.io/4.8.3/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.8.3/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.8.3/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Micronaut Maven Plugin documentation](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/)
## Feature micronaut-aot documentation

- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)


## Feature serialization-jackson documentation

- [Micronaut Serialization Jackson Core documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)


## Feature maven-enforcer-plugin documentation

- [https://maven.apache.org/enforcer/maven-enforcer-plugin/](https://maven.apache.org/enforcer/maven-enforcer-plugin/)

## Windows port process cleanup

- #netstat -aon | findstr :8080
- #taskkill /PID 12345 /F

## Windows port process cleanup
- how to exec the db, commands
-  1- jpa+persistence api single postgress
  - - add httpresponse instead
  - - add/delete/update vet
  - - add/delete/update specialty
  - - how jpa handles intermediate table
-   -   add jakarta validations -> custom annotation for validating createVet with specialty, that specialty exists.
-   -   add javadoc
-  2- jpa+persistence api with entity manager for custom queries single postgress
-  3- jpa+persistence api with entity manager for custom queries and usage of multiple db postgress configurtion
-  4- Micronaut Data JDBC (Compile-Time ORM)
-  5- Micronaut Data R2DBC (Reactive)



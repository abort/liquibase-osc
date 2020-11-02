# Online Schema Change
Adds support to Liquibase for online schema changes. These are DDL statements that imply little to no locking, such that DML operations can continue. Currently only Oracle is supported.

## How to build
Use `mvn install` to get a jar.

## How to use
Add the jar to your spring-boot liquibase application. Set a property in your `databaseChangeLog`:
```
  - property:
      name: auto-online-ddl
      value: true
      global: false
```

This property is false by default and therefore does not change any behaviour by default. This allows the plugin to be present in cases where the database is not supported (or the database does not support online DDL changes). Furthermore it provides a form of granularity to do online and non-online schema changes, without requiring new types of changes or new XML schema definitions. Therefore this route tries to minimize maintainence when future Liquibase versions get released.

To find out more about the generated statements (and to see if they actually generate online DDL), set:
`logging.level.liquibase=DEBUG` in your spring-boot application properties file.


## Supported existing constructs 
This automatically takes care of the regular DDL statements and replaces those with their onlien equivalent. Currently supported are:

* Add Column
* Create Index
* Drop Column
* Drop Foreign Key Constraint
* Drop Primary Key Constraint
* Drop Unique Constraint

## Behaviour of the property
Liquibase properties work a bit quirky. By default properties are global and propagate everywhere (not just downwars in the changelog hierarchy). Do not use `global` because that is a bad practice.

We consider the following structure to look at two scenarios:

```
resources
│   
│
└───liquibase
    │   db.changelog-master.yaml
    │
    └───v1
	│   │   first_changes.yaml
	│
	│   
	└───v2
	    │   second_changes.yaml
```

Each yaml file contains a `databaseChangeLog`. The `db.changelog-master.yaml` file simply includes the other `databaseChangeLog` yaml files in order.

### Scenario 1
In case the files have the following setup:

```
resources
│   
│
└───liquibase
    │   db.changelog-master.yaml: auto-online-ddl: true, global: false
    │
    └───v1
	│   │   first_changes.yaml
	│
	│   
	└───v2
	    │   second_changes.yaml
```

Then changes in `v1` and `v2` will **not** result in online schema change statements.

### Scenario 2
```
resources
│   
│
└───liquibase
    │   db.changelog-master.yaml: auto-online-ddl: false, global: false
    │
    └───v1
	│   │   first_changes.yaml: auto-online-ddl: true, global: false
	│
	│   
	└───v2
	    │   second_changes.yaml
```

Then changes in `v1` will result in online schema change statements. Changes in `v2` will be left unchanged.


### Summary
When a property is not global, it only is in the scope of the local `databaseChangeLog` and not to its children. When global is set, it is immutable from a local scope.
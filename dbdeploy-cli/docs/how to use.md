# preparations - jars

1. build dbdeploy-cli jar using gradle: `garadle fatJar` or gradlew: `dbdeploy/gradlew fatJar` (`gradlew.bat` on windows)
2. find `dbdeploy-cli-all` jar at `dbdeploy/dbdeploy-cli/build/libs`
3. find db driver jar (probably, one should download it manually from something like `search.maven.org`)
4. find patches folder
5. put 2-5 into same directory


# preparations - database

* DB should have user with sufficient permissions to execute all the sql patches
* DB should have changelog table(s) like this: 
```sql
CREATE TABLE changelog_table_name (
    change_number   NUMBER      NOT NULL PRIMARY KEY, 
    complete_dt     DATE/NUMBER NOT NULL, 
    applied_by      STRING      NOT NULL, 
    description     STRING      NOT NULL 
);
```
_(exact table definition varies based on target DB syntax)_

# running

* run jars with command like this:

```bash
#driverPath is path to the sql driver jar
#dbdeployPath is path to dbdeploy-cli jar
 
java -cp driverPath;dbdeployPath com.dbdeploy.CommandLineTarget
--driver "org.hsqldb.jdbcDriver" \
--url "jdbc:hsqldb:file:db/whatever" \
--userid "whatever" \
--password "" \
--scriptdirectory "meh" #default is . \
--changeLogTableName "foobar" #default is changelog or automatically inferred based on patches sub-folders names \
--dbms "pgsql" #sql dialect
```

_**NB:** if you don't want to include the database password on the
command line (as you probably don't), miss out the parameter to
--password and dbdeploy will then read from stdin._


_Also: one could run `gradle cleanTest test --tests *JarIntegrationTest -i` under `dbdeploy/dbdeploy-cli` dir
to get insight on what the actual commands being executed_

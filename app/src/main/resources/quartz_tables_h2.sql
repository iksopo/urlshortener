-- Note, Quartz depends on row-level locking which means you must use the MVC=TRUE
-- setting on your H2 database, or you will experience dead-locks
--
-- In your Quartz properties file, you'll need to set
-- org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.StdJDBCDelegate

CREATE TABLE QRTZ_CALENDARS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  CALENDAR_NAME VARCHAR (200)  NOT NULL ,
  CALENDAR IMAGE NOT NULL
);

...
-- Download the entire script from our GitHub repository
...

COMMIT;
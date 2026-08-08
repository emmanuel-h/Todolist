# Keep Room entity and DAO classes so R8 doesn't rename the fields that map
# to database columns or the method names that Room's generated code calls.
-keep class fr.mandarine.todolist.data.** { *; }

# Keep domain model classes intact — their field names are used in logs and
# may be referenced from data layer generated code.
-keep class fr.mandarine.todolist.domain.** { *; }

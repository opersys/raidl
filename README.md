# Reverse AIDL tool

This is an extension command to AOSP sources so it has to be build as AOSP source. To build this,
copy or link the repository in the "external" repository of your AOSP source. It will become part
of your build.

# Example output

```
# raidl iface alarm

// Service: alarm, Interface: android.app.IAlarmManager
package android.app;

interface IAlarmManager {
    void set(int n1, long n2, PendingIntent p3);
    void setRepeating(int n1, long n2, long n3, PendingIntent p4);
    void setInexactRepeating(int n1, long n2, long n3, PendingIntent p4);
    void setTime(long n1);
    void setTimeZone(String s1);
    void remove(PendingIntent p1);
}
```

```
# raidl iface alarm -l
Class: android.app.IAlarmManager

   1	void set(int n1, long n2, android.app.PendingIntent p3);
   2	void setRepeating(int n1, long n2, long n3, android.app.PendingIntent p4);
   3	void setInexactRepeating(int n1, long n2, long n3, android.app.PendingIntent p4);
   4	void setTime(long n1);
   5	void setTimeZone(java.lang.String s1);
   6	void remove(android.app.PendingIntent p1);
```

# Contributors

* François-Denis Gonthier francois-denis.gonthier@opersys.com -- main developer and maintainer
* Karim Yaghmour karim.yaghmour@opersys.com -- ideas and other forms of entertainment
# IntelliJ IDEA Guide

This guide helps you set up InteliJ IDEA environment.

### Import project

Please import the semux project as a Maven project.

### Java code style

Please import the provided [Eclipse Java formatter](https://raw.githubusercontent.com/semuxproject/semux/master/misc/eclipse/formatter.xml).

### Organize imports

In `Settings`/`Preference` > `Code Style` > `Java` > `imports`,

* In general,
    - Set `Class count to use import with '*'` to `99`;
    - Set `Names count to use static import with '*'` to `99`;
* In `Packages to Use Import with '*'`:
    - Set the none
* In `Import layout`:
    - Enable `Layout static import separately`
    - Change the list to
        * `import static all other imports`
        * blank line
        * `import java.*`
        * blank line
        * `import javax.*`
        * blank line
        * `import org.*`
        * blank line
        * `import com.*`
        * blank line
        * `import all other imports`

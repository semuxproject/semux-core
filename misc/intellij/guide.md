# IntelliJ IDEA Setup Guide

This guide helps you set up InteliJ IDEA environment.

### Import the project

1. Clone the semux project via `git clone https://github.com/semuxproject/semux`;
2. Open IntelliJ IDEA and import it as a Maven project.

### Set up code stye

1. Download the [Eclipse formatter XML](https://raw.githubusercontent.com/semuxproject/semux/master/misc/eclipse/formatter.xml);
2. Go to `Preferences` > `Code Style` > `Java`, click the `gear icon` and import the downloaded schema;
3. Click the `Imports` tab:
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
4. Now you're workspace is ready!

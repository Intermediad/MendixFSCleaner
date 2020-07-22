# MendixFSCleaner
Tool to cleanup orphaned files in Mendix filestore by comparing filenames with `__uuid__` in `System.FileDocument`. 
These files only reside in the filestore and have no association to any object in the database.

## Requires
Java JDK 1.8 or higher

## Usage
`java -jar mxfilecleaner.jar -h localhost -d mydatabase -u postgres -p 5432 -path "c:\someproject\deployment\data\files"`

Add option `-log` to create a `missing_files.log` and `-delete` to actually delete the orphaned files

## Commandline options
| Option              | Required* | Description                                                         |
| ------------------- | :-------: | :------------------------------------------------------------------ | 
| `-h <dbhost>`       | Yes       | Database hostname                                                   |
| `-d <dbname>`       | Yes       | Database name                                                       |
| `-u <dbuser>`       | Yes       | Database username                                                   |
| `-P <dbport>`       | No        | Database port - defaults to 5432                                    |
| `-path <filepath>`  | Yes       | Full path to Mendix files (ie c:\someproject\deployment\data\files) |
| `-log`              | No        | Log missing files to missing_files.log                              |
| `-delete`           | No        | Delete file missing in database from filesystem                     |

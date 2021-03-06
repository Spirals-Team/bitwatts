# BitWatts

[![Join the chat at https://gitter.im/Spirals-Team/bitwatts](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Spirals-Team/bitwatts?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/Spirals-Team/bitwatts.svg)](https://travis-ci.org/Spirals-Team/bitwatts)
[![Coverage Status](https://coveralls.io/repos/Spirals-Team/bitwatts/badge.svg)](https://coveralls.io/r/Spirals-Team/bitwatts)
[![Codacy Badge](https://www.codacy.com/project/badge/d2e5e4f39ff248439b1968d45aa45811)](https://www.codacy.com/app/mcolmant/bitwatts)

BitWatts is an extension of [PowerAPI](https://github.com/Spirals-Team/powerapi) for building software-defined power meters inside virtualized environments.
To have more details on what is a software-defined power meter, please consult: http://powerapi.org.

# About
BitWatts is an open-source project developed by the [Spirals research group](https://team.inria.fr/spirals) (University of Lille 1 and Inria) in collaboration with the [University of Neuchâtel](http://www2.unine.ch/).

The research leading to these results has received funding from the European Community’s Seventh Framework Programme [FP7/2007- 2013] under the [ParaDIME Project](http://paradime-project.eu/), grant agreement no. 318693.

This project is fully managed with [sbt](http://www.scala-sbt.org/).

The documentation is available [here](https://github.com/Spirals-Team/bitwatts/wiki).

## Mailing list
You can follow the latest news and asks questions by subscribing to our <a href="mailto:sympa@inria.fr?subject=subscribe powerapi">mailing list</a>.

## Publications
* **[Process-level Power Estimation in VM-based Systems](https://hal.inria.fr/hal-01130030)**: M. Colmant, M. Kurpicz, L. Huertas, R. Rouvoy, P. Felber, A. Sobe. *European Conference on Computer Systems* (EuroSys). April 2015, Bordeaux, France. pp.1-14. To appear.

## Contributing
If you would like to contribute code you can do so through GitHub by forking the repository and sending a pull request.

When submitting code, please make every effort to follow existing conventions and style in order to keep the code as readable as possible.

## Acknowledgments
We all stand on the shoulders of giants and get by with a little help from our friends. BitWatts is written in [Scala](http://www.scala-lang.org) (version 2.11.4 under [3-clause BSD license](http://www.scala-lang.org/license.html)) and built on top of:
* [PowerAPI](https://github.com/Spirals-Team/powerapi) (version 3.2 under [AGPL license](http://www.gnu.org/licenses/agpl-3.0.html)), for using a software-defined power meter
* [JUnixSocket](https://code.google.com/p/junixsocket/) (version 1.3 under [Apache 2 license](http://www.apache.org/licenses/LICENSE-2.0)), for using unix domain sockets (quick time accesses)
* [Apache Thrift](https://thrift.apache.org/) (version 0.9.2 under [Apache 2 license](http://www.apache.org/licenses/LICENSE-2.0)), for scalable cross-language services
* [JeroMQ](https://github.com/zeromq/jeromq) (version 0.3.4 under [LGPL3 license](https://github.com/zeromq/jeromq/blob/master/COPYING.LESSER)), for using libzmq in native java.
* [Apache log4j2](http://logging.apache.org/log4j/2.x) (version 2.3 under [Apache 2 license](http://www.apache.org/licenses/LICENSE-2.0)), for logging.

# License
This software is licensed under the *GNU Affero General Public License*, quoted below.

Copyright (C) 2011-2015 Inria, University of Lille 1, University of Neuchâtel.

BitWatts is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

BitWatts is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with BitWatts. If not, please consult http://www.gnu.org/licenses/agpl-3.0.html.

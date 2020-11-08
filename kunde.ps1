# Copyright (C) 2017 -  Juergen Zimmermann, Hochschule Karlsruhe
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

# Aufruf:   kunde [cmd=start|stop|shutdown]

# "Param" muss in der 1. Zeile sein
Param (
    [string]$cmd = "start"
)

Set-StrictMode -Version Latest

# Titel setzen
$script = $myInvocation.MyCommand.Name
$host.ui.RawUI.WindowTitle = $script

function StartServer {
    ./gradlew
}

function StopServer {
    $schema = "https"
    $tls = "--tlsv1.3"

    #$http3 = "--http3"
    $http3 = "--http2"

    $port = "8444"
    $username = "admin"
    $password = "p"

    $url = "${schema}://localhost:${port}/actuator/shutdown"
    $basicAuth = "${username}:${password}"

    C:\Zimmermann\Git\mingw64\bin\curl --verbose --request POST --basic --user $basicAuth $tls $http3 --insecure $url
}

switch ($cmd) {
    "start" { StartServer; break }
    "stop" { StopServer; break }
    "shutdown" { StopServer; break }
    default { write-host "$script [cmd=start|stop|shutdown]" }
}

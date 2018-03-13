[![Build Status](https://api.travis-ci.org/symbiote-h2020/EnablerLogicGreenRouteController.svg?branch=staging)](https://api.travis-ci.org/symbiote-h2020/EnablerLogicGreenRouteController)
[![codecov.io](https://codecov.io/github/symbiote-h2020/EnablerLogicGreenRouteController/branch/master/graph/badge.svg)](https://codecov.io/github/symbiote-h2020/EnablerLogicGreenRouteController)

# Enabler Logic - Green Route Controller

There are certain configurations that need to be set before starting this component. These configurations can be found in the bootstrap.properties file. They involve stating the regions to be considered, where can the files for those regions be found and their respective formats and the properties (and their IRI) that are expected in each region. Additionally, it is necessary to declare the routing services that are going to be used, stating, additionally, what regions they are interested in, if they are an internal or external service and, in case that is true, where to find their interfaces. 

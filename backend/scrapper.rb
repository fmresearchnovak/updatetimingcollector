#!/usr/bin/env ruby
require "market_bot"


# Author: Prof. Novak
# Description: This is a ruby app that uses the market_bot ruby gem to
# scrape the google play store.  You call it directly with an app's
# package name to have it retrieve some info about that app
# e.g., ruby scrapper.rb com.facebook.katana

# It is used by scrapper.py (call that script peferably)
# To gather this information for many apps.

app = MarketBot::Play::App.new(ARGV[0])

app.update

# Print out the app title.
puts app.title

# Print all the other attributes you can find on an app object.
#puts MarketBot::Play::App::ATTRIBUTES.inspect

# Print attributes I care about
puts app.current_version
puts app.updated

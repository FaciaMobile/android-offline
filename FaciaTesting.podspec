Pod::Spec.new do |spec|
  spec.name         = "Facia"
  spec.version      = "1.0.5"
  spec.summary      = "iOS-SDK"
  spec.description  = <<-DESC
  Facia Core SDK
  DESC

  spec.homepage       = "https://github.com/FaciaMobile/android-offline"
  spec.license      = { :type => "MIT", :file => "LICENSE" }
  spec.author       = { "Facia" => "support@facia.ai" }

  spec.platform     = :ios, "13.0"
  spec.swift_version = "5"

  spec.source       = { :git => "https://github.com/FaciaMobile/android-offline.git", :tag => "#{spec.version}" }

  spec.module_name  = "Facia"
  spec.ios.vendored_frameworks = "FaciaTesting.xcframework"
end

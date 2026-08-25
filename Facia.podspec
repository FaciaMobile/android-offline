Pod::Spec.new do |spec|

  spec.name         = "Facia"
  spec.version      = "1.0.0"
  spec.summary      = "iOS-SDK"
  
  spec.description  = <<-DESC
  Facia Core SDK
  DESC
  
  spec.homepage     = "https://github.com/FaciaMobile/ios-core"
  spec.license      = { :type => "MIT", :file => "LICENSE" }
  spec.author       = { "Facia" => "support@facia.ai" }
  spec.platform     = :ios, "11.0"
  
  spec.source       = { :git => "https://github.com/FaciaMobile/ios-core.git", :tag => "#{spec.version}" }
  
    spec.ios.vendored_frameworks = 'Facia.xcframework'
    spec.swift_version = "5"
  
  spec.exclude_files = "Classes/Exclude"
  spec.pod_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'arm64' }
  spec.user_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'arm64' }
  end

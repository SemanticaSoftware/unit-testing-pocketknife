# unit-testing-pocketknife
A small and sharp toolset for unit testing and mocking dependencies in Java
<p align="center">
<img width="400" height="432" src="https://raw.githubusercontent.com/ahaanstra/unit-testing-pocketknife/master/img/Swiss-Army-Knife.png">
</p>

  [**For any issues, please post to the main (Semantica) repository !!!**](https://github.com/SemanticaSoftware/unit-testing-pocketknife "Main project repository")
  
<p>
  This library was written to facilate mock call verification on self made mock objects. It tries to provide its users with a lightweight, intuitive, clear-cut and transparent api. It only uses some magic (byte code generation) to facilitate capturing method calls for mock verification. Features as the use of Hamcrest Matchers, Predicates and strict verification for method invocation are supported. This library especially facilitates making more complicated mocks as the mock logic is to be defined in an object-oriented way in the mock class itself. *Also, for simple mock objects, it provides a clean-cut api (coming soon).*
  
  In the near future, an examples project will be linked where the api is illustrated and some mocking templates are provided.
  
  **This library is very much in-development at this moment and it is therefore not recommended to use it to test production code.**
</p>

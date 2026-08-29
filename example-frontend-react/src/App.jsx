import React, {Component} from 'react';
import './App.css';
import Hello from "./components/Hello";

class App extends Component {

  render() {
    return (
        <Hello logout={this.props.logout}/>
    );
  }
}

export default App;

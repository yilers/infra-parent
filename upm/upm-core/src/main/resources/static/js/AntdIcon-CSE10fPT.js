import{E as e,N as t,O as n,P as r,St as i,gt as a,j as o,k as s,wt as c,z as l}from"./vue.runtime.esm-bundler-D9lAnbr7.js";import{C as u,w as d}from"./useConfigInject-BLVOP0Qm.js";t(),a();var f=Symbol(`iconContext`),p=function(){return r(f,{prefixCls:c(`anticon`),rootClassName:c(``),csp:c()})};function m(){return!!(typeof window<`u`&&window.document&&window.document.createElement)}function h(e,t){return e&&e.contains?e.contains(t):!1}var g=`data-vc-order`,_=`vc-icon-key`,v=new Map;function y(){var e=(arguments.length>0&&arguments[0]!==void 0?arguments[0]:{}).mark;return e?e.startsWith(`data-`)?e:`data-${e}`:_}function b(e){return e.attachTo?e.attachTo:document.querySelector(`head`)||document.body}function x(e){return e===`queue`?`prependQueue`:e?`prepend`:`append`}function S(e){return Array.from((v.get(e)||e).children).filter(function(e){return e.tagName===`STYLE`})}function C(e){var t=arguments.length>1&&arguments[1]!==void 0?arguments[1]:{};if(!m())return null;var n=t.csp,r=t.prepend,i=document.createElement(`style`);i.setAttribute(g,x(r)),n&&n.nonce&&(i.nonce=n.nonce),i.innerHTML=e;var a=b(t),o=a.firstChild;if(r){if(r===`queue`){var s=S(a).filter(function(e){return[`prepend`,`prependQueue`].includes(e.getAttribute(g))});if(s.length)return a.insertBefore(i,s[s.length-1].nextSibling),i}a.insertBefore(i,o)}else a.appendChild(i);return i}function w(e){var t=arguments.length>1&&arguments[1]!==void 0?arguments[1]:{};return S(b(t)).find(function(n){return n.getAttribute(y(t))===e})}function T(e,t){var n=v.get(e);if(!n||!h(document,n)){var r=C(``,t),i=r.parentNode;v.set(e,i),e.removeChild(r)}}function E(e,t){var n=arguments.length>2&&arguments[2]!==void 0?arguments[2]:{};T(b(n),n);var r=w(t,n);if(r)return n.csp&&n.csp.nonce&&r.nonce!==n.csp.nonce&&(r.nonce=n.csp.nonce),r.innerHTML!==e&&(r.innerHTML=e),r;var i=C(e,n);return i.setAttribute(y(n),t),i}t();function D(e){for(var t=1;t<arguments.length;t++){var n=arguments[t]==null?{}:Object(arguments[t]),r=Object.keys(n);typeof Object.getOwnPropertySymbols==`function`&&(r=r.concat(Object.getOwnPropertySymbols(n).filter(function(e){return Object.getOwnPropertyDescriptor(n,e).enumerable}))),r.forEach(function(t){O(e,t,n[t])})}return e}function O(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function ee(e,t){`${t}`}function k(e){return typeof e==`object`&&typeof e.name==`string`&&typeof e.theme==`string`&&(typeof e.icon==`object`||typeof e.icon==`function`)}function A(e,t,n){return n?o(e.tag,D({key:t},n,e.attrs),(e.children||[]).map(function(n,r){return A(n,`${t}-${e.tag}-${r}`)})):o(e.tag,D({key:t},e.attrs),(e.children||[]).map(function(n,r){return A(n,`${t}-${e.tag}-${r}`)}))}function j(e){return d(e)[0]}function M(e){return e?Array.isArray(e)?e:[e]:[]}var te=`
.anticon {
  display: inline-block;
  color: inherit;
  font-style: normal;
  line-height: 0;
  text-align: center;
  text-transform: none;
  vertical-align: -0.125em;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.anticon > * {
  line-height: 1;
}

.anticon svg {
  display: inline-block;
}

.anticon::before {
  display: none;
}

.anticon .anticon-icon {
  display: block;
}

.anticon[tabindex] {
  cursor: pointer;
}

.anticon-spin::before,
.anticon-spin {
  display: inline-block;
  -webkit-animation: loadingCircle 1s infinite linear;
  animation: loadingCircle 1s infinite linear;
}

@-webkit-keyframes loadingCircle {
  100% {
    -webkit-transform: rotate(360deg);
    transform: rotate(360deg);
  }
}

@keyframes loadingCircle {
  100% {
    -webkit-transform: rotate(360deg);
    transform: rotate(360deg);
  }
}
`;function N(e){return e&&e.getRootNode&&e.getRootNode()}function ne(e){return m()?N(e)instanceof ShadowRoot:!1}function re(e){return ne(e)?N(e):null}var ie=function(){var e=p(),t=e.prefixCls,n=e.csp,r=s(),i=te;t&&(i=i.replace(/anticon/g,t.value)),l(function(){if(m()){var e=r.vnode.el,t=re(e);E(i,`@ant-design-vue-icons`,{prepend:!0,csp:n.value,attachTo:t})}})};a();var ae=[`icon`,`primaryColor`,`secondaryColor`];function P(e,t){if(e==null)return{};var n=F(e,t),r,i;if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);for(i=0;i<a.length;i++)r=a[i],!(t.indexOf(r)>=0)&&Object.prototype.propertyIsEnumerable.call(e,r)&&(n[r]=e[r])}return n}function F(e,t){if(e==null)return{};for(var n={},r=Object.keys(e),i,a=0;a<r.length;a++)i=r[a],!(t.indexOf(i)>=0)&&(n[i]=e[i]);return n}function I(e){for(var t=1;t<arguments.length;t++){var n=arguments[t]==null?{}:Object(arguments[t]),r=Object.keys(n);typeof Object.getOwnPropertySymbols==`function`&&(r=r.concat(Object.getOwnPropertySymbols(n).filter(function(e){return Object.getOwnPropertyDescriptor(n,e).enumerable}))),r.forEach(function(t){L(e,t,n[t])})}return e}function L(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}var R=i({primaryColor:`#333`,secondaryColor:`#E6E6E6`,calculated:!1});function z(e){var t=e.primaryColor,n=e.secondaryColor;R.primaryColor=t,R.secondaryColor=n||j(t),R.calculated=!!n}function B(){return I({},R)}var V=function(e,t){var n=I({},e,t.attrs),r=n.icon,i=n.primaryColor,a=n.secondaryColor,o=P(n,ae),s=R;if(i&&(s={primaryColor:i,secondaryColor:a||j(i)}),ee(k(r),`icon should be icon definiton, but got ${r}`),!k(r))return null;var c=r;return c&&typeof c.icon==`function`&&(c=I({},c,{icon:c.icon(s.primaryColor,s.secondaryColor)})),A(c.icon,`svg-${c.name}`,I({},o,{"data-icon":c.name,width:`1em`,height:`1em`,fill:`currentColor`,"aria-hidden":`true`}))};V.props={icon:Object,primaryColor:String,secondaryColor:String,focusable:String},V.inheritAttrs=!1,V.displayName=`IconBase`,V.getTwoToneColors=B,V.setTwoToneColors=z;function H(e,t){return q(e)||K(e,t)||W(e,t)||U()}function U(){throw TypeError(`Invalid attempt to destructure non-iterable instance.
In order to be iterable, non-array objects must have a [Symbol.iterator]() method.`)}function W(e,t){if(e){if(typeof e==`string`)return G(e,t);var n=Object.prototype.toString.call(e).slice(8,-1);if(n===`Object`&&e.constructor&&(n=e.constructor.name),n===`Map`||n===`Set`)return Array.from(e);if(n===`Arguments`||/^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n))return G(e,t)}}function G(e,t){(t==null||t>e.length)&&(t=e.length);for(var n=0,r=Array(t);n<t;n++)r[n]=e[n];return r}function K(e,t){var n=e==null?null:typeof Symbol<`u`&&e[Symbol.iterator]||e[`@@iterator`];if(n!=null){var r=[],i=!0,a=!1,o,s;try{for(n=n.call(e);!(i=(o=n.next()).done)&&(r.push(o.value),!(t&&r.length===t));i=!0);}catch(e){a=!0,s=e}finally{try{!i&&n.return!=null&&n.return()}finally{if(a)throw s}}return r}}function q(e){if(Array.isArray(e))return e}function J(e){var t=H(M(e),2),n=t[0],r=t[1];return V.setTwoToneColors({primaryColor:n,secondaryColor:r})}function oe(){var e=V.getTwoToneColors();return e.calculated?[e.primaryColor,e.secondaryColor]:e.primaryColor}t();var se=n({name:`InsertStyles`,setup:function(){return ie(),function(){return null}}});t();var ce=[`class`,`icon`,`spin`,`rotate`,`tabindex`,`twoToneColor`,`onClick`];function le(e,t){return fe(e)||de(e,t)||Y(e,t)||ue()}function ue(){throw TypeError(`Invalid attempt to destructure non-iterable instance.
In order to be iterable, non-array objects must have a [Symbol.iterator]() method.`)}function Y(e,t){if(e){if(typeof e==`string`)return X(e,t);var n=Object.prototype.toString.call(e).slice(8,-1);if(n===`Object`&&e.constructor&&(n=e.constructor.name),n===`Map`||n===`Set`)return Array.from(e);if(n===`Arguments`||/^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n))return X(e,t)}}function X(e,t){(t==null||t>e.length)&&(t=e.length);for(var n=0,r=Array(t);n<t;n++)r[n]=e[n];return r}function de(e,t){var n=e==null?null:typeof Symbol<`u`&&e[Symbol.iterator]||e[`@@iterator`];if(n!=null){var r=[],i=!0,a=!1,o,s;try{for(n=n.call(e);!(i=(o=n.next()).done)&&(r.push(o.value),!(t&&r.length===t));i=!0);}catch(e){a=!0,s=e}finally{try{!i&&n.return!=null&&n.return()}finally{if(a)throw s}}return r}}function fe(e){if(Array.isArray(e))return e}function Z(e){for(var t=1;t<arguments.length;t++){var n=arguments[t]==null?{}:Object(arguments[t]),r=Object.keys(n);typeof Object.getOwnPropertySymbols==`function`&&(r=r.concat(Object.getOwnPropertySymbols(n).filter(function(e){return Object.getOwnPropertyDescriptor(n,e).enumerable}))),r.forEach(function(t){Q(e,t,n[t])})}return e}function Q(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function pe(e,t){if(e==null)return{};var n=me(e,t),r,i;if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);for(i=0;i<a.length;i++)r=a[i],!(t.indexOf(r)>=0)&&Object.prototype.propertyIsEnumerable.call(e,r)&&(n[r]=e[r])}return n}function me(e,t){if(e==null)return{};for(var n={},r=Object.keys(e),i,a=0;a<r.length;a++)i=r[a],!(t.indexOf(i)>=0)&&(n[i]=e[i]);return n}J(u.primary);var $=function(t,n){var r,i=Z({},t,n.attrs),a=i.class,o=i.icon,s=i.spin,c=i.rotate,l=i.tabindex,u=i.twoToneColor,d=i.onClick,f=pe(i,ce),m=p(),h=m.prefixCls,g=m.rootClassName,_=(r={},Q(r,g.value,!!g.value),Q(r,h.value,!0),Q(r,`${h.value}-${o.name}`,!!o.name),Q(r,`${h.value}-spin`,!!s||o.name===`loading`),r),v=l;v===void 0&&d&&(v=-1);var y=c?{msTransform:`rotate(${c}deg)`,transform:`rotate(${c}deg)`}:void 0,b=le(M(u),2),x=b[0],S=b[1];return e(`span`,Z({role:`img`,"aria-label":o.name},f,{onClick:d,class:[_,a],tabindex:v}),[e(V,{icon:o,primaryColor:x,secondaryColor:S,style:y},null),e(se,null,null)])};$.props={spin:Boolean,rotate:Number,icon:Object,twoToneColor:[String,Array]},$.displayName=`AntdIcon`,$.inheritAttrs=!1,$.getTwoToneColor=oe,$.setTwoToneColor=J;export{$ as t};